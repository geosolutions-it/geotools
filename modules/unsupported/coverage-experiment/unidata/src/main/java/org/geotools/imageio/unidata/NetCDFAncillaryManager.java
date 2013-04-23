package org.geotools.imageio.unidata;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.io.catalog.CoverageSlice;
import org.geotools.coverage.io.util.Utilities;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.catalog.index.Indexer;
import org.geotools.gce.imagemosaic.catalog.index.Indexer.Collectors;
import org.geotools.gce.imagemosaic.catalog.index.Indexer.Collectors.Collector;
import org.geotools.gce.imagemosaic.catalog.index.Indexer.Coverages;
import org.geotools.gce.imagemosaic.catalog.index.Indexer.Coverages.Coverage;
import org.geotools.gce.imagemosaic.catalog.index.ObjectFactory;
import org.geotools.gce.imagemosaic.catalog.index.SchemaType;
import org.geotools.gce.imagemosaic.catalog.index.SchemasType;
import org.geotools.gce.imagemosaic.properties.DefaultPropertiesCollectorSPI;
import org.geotools.gce.imagemosaic.properties.PropertiesCollector;
import org.geotools.gce.imagemosaic.properties.PropertiesCollectorFinder;
import org.geotools.gce.imagemosaic.properties.PropertiesCollectorSPI;
import org.geotools.imageio.unidata.UnidataImageReader.DatastoreProperties;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** 
 * A class used to store any auxiliary indexing information
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
class NetCDFAncillaryManager {

    private final static Logger LOGGER = Logging.getLogger(NetCDFAncillaryManager.class.toString());

    private static ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private static Marshaller MARSHALLER;

    private static Unmarshaller UNMARSHALLER;

    static final String DEFAULT = "default";
    
    private static final Set<PropertiesCollectorSPI> pcSPIs = PropertiesCollectorFinder.getPropertiesCollectorSPI();

    static {
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance("org.geotools.gce.imagemosaic.catalog.index");
            MARSHALLER = jc.createMarshaller();
            UNMARSHALLER = jc.createUnmarshaller();
        } catch (JAXBException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    Indexer indexer;

    private static final String INDEX_SUFFIX = ".xml";

    private static final String COVERAGE_NAME = "coverageName";

    public NetCDFAncillaryManager(final File file, final String indexFilePath) {
        org.geotools.util.Utilities.ensureNonNull("file", file);
        if (!file.exists()) {
            throw new IllegalArgumentException("The specified file doesn't exist: " + file);
        }

        // Set files  
        mainFile = file;
        parentDir = new File(mainFile.getParent());

        // Look for external folder configuration
        final String baseFolder = UnidataUtilities.EXTERNAL_DATA_DIR;
        File baseDir = null;
        if (baseFolder != null) {
            baseDir = new File(baseFolder);
            // Check it again in case it has been deleted in the meantime:
            baseDir = UnidataUtilities.isValid(baseDir) ? baseDir : null;
        }

        try {
            String mainFilePath = mainFile.getCanonicalPath();
            String baseName = FilenameUtils.removeExtension(FilenameUtils.getName(mainFilePath));
            String outputLocalFolder = "." + baseName;
            destinationDir = new File(parentDir, outputLocalFolder);

            // append base file folder tree to the optional external data dir
            if (baseDir != null) {
                destinationDir = new File(baseDir, outputLocalFolder);
            }

            boolean createdDir = false;
            if (!destinationDir.exists()) {
                createdDir = destinationDir.mkdirs();
            }

            // Init auxiliary file names
            slicesIndexFile = new File(destinationDir, baseName + ".idx");
            if (indexFilePath != null) {
                indexerFile = new File(indexFilePath);
                if (!indexerFile.exists() || !indexerFile.canRead()) {
                    indexerFile = null;
                }
            }
            if (indexerFile == null) {
                indexerFile = new File(destinationDir, baseName + INDEX_SUFFIX);
            }
            slicesIndexList = new ArrayList<UnidataSlice2DIndex>();
            
            if (!createdDir) {
                // Check for index to be reset only in case we didn't created a new directory.
                checkReset(mainFile, slicesIndexFile, destinationDir);
            }

        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        } 
    }

    /**
     * Check whether the Net
     * @param mainFile
     * @param slicesIndexFile
     * @param destinationDir
     * @throws IOException
     */
    private static void checkReset(final File mainFile, final File slicesIndexFile, final File destinationDir) throws IOException {
        // TODO: Consider acquiring a LOCK on the file
        if (slicesIndexFile.exists()) {
            final long mainFileTime = mainFile.lastModified();
            final long indexTime = slicesIndexFile.lastModified();

            // Check whether the NetCDF time is more recent with respect to the auxiliary indexes
            if (mainFileTime > indexTime) {
                // Need to delete all the auxiliary files and start from scratch
                final Collection<File> listedFiles = FileUtils.listFiles(destinationDir, null, true);
                for (File file: listedFiles) {

                    // Preserve summary file which contains mapping between coverages and underlying variables
                    if (!file.getAbsolutePath().endsWith(INDEX_SUFFIX)) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        }
    }

    /**
     * The list of Slice2D indexes
     */
    protected List<UnidataSlice2DIndex> slicesIndexList;
    
    /** 
     * The Slice2D index manager
     */
    protected UnidataSlice2DIndexManager slicesIndexManager;

    /** 
     * The list of coverages to be put in the coverages summary file
     */
    
    // TODO: Refactor these mappings in favor of using the Indexer objects if possible
    
    // Map variable names to coverage names
    private Map<String, Name> coveragesMap = null;
    
    // Map coverage names to original NetCDF variable (it's the reverse mapping) 
    Map<Name, String> variablesMap = null;
    
    // Map coverages to schema typeNames
    Map<String, String> coveragesToSchemaMap = new HashMap<String, String>();
    
    // Map schema names to schema attributes string
    Map<String, String> schemaMapping = new HashMap<String, String>();

    /**
     * An optional PropertiesCollector to extract the coverage name from the file name
     */
    private Map<String, PropertiesCollector> collectors = null;
    
    private File parentDir;

    private File destinationDir;

    /**
     * The main NetCDF file
     */
    private File mainFile;

    /** 
     * File storing the slices index (index, Tsection, Zsection) 
     */
    private File slicesIndexFile;

    /**
     * File storing the coverage names summary. This will allow knowing the available coverages name without opening and scanning again all the
     * dataset
     */
    private File indexerFile;
    
    /** 
     * The datastore containing the {@link CoverageSlice} index
     */
    protected Properties datastoreProps = null;
    
    public void writeToDisk() throws IOException, JAXBException {
        // Write collected information
        UnidataSlice2DIndexManager.writeIndexFile(slicesIndexFile, slicesIndexList);
        if (!indexerFile.exists()) {
            storeIndexer(indexerFile, coveragesMap);
        }
    }

    /**
     * Write to disk the variable summary, a simple text file containing variable names.
     * 
     * @param variablesSummaryFile
     * @param coveragesMapping
     * @throws JAXBException 
     */
    private void storeIndexer(final File variablesSummaryFile,
            final Map<String, Name> coveragesMapping) throws JAXBException {
        if (coveragesMapping == null || coveragesMapping.isEmpty()) {
            throw new IllegalArgumentException("No valid coverages name to be written");
        }
        
        // Create the main indexer
        Indexer indexer = OBJECT_FACTORY.createIndexer();
        Coverages coverages = OBJECT_FACTORY.createIndexerCoverages();
        SchemasType schemas = OBJECT_FACTORY.createSchemasType();
        indexer.setCoverages(coverages);
        indexer.setSchemas(schemas);
        
        // create schemas reference
        if (schemaMapping.containsKey(DEFAULT)) {
            SchemaType schema = OBJECT_FACTORY.createSchemaType();
            schema.setName(DEFAULT);
            schema.setAttributes(schemaMapping.get(DEFAULT));
            schemas.getSchema().add(schema);
        }
        
        // create coverages
        List<Coverage> coveragesList = coverages.getCoverage();
        Set<String> keys = coveragesMapping.keySet();
        for (String key: keys) {
            Coverage coverage = OBJECT_FACTORY.createIndexerCoveragesCoverage();
            coverage.setName(key);
            coveragesList.add(coverage);
            if (coveragesToSchemaMap.containsKey(key)) {
                String schemaTypeName = coveragesToSchemaMap.get(key);
                if (schemaTypeName.equalsIgnoreCase(DEFAULT)) {
                    SchemaType schema = OBJECT_FACTORY.createSchemaType();
                    schema.setRef(schemaTypeName);
                    coverage.setSchema(schema);
                }
            }
        }
        MARSHALLER.marshal(indexer, variablesSummaryFile);
    }

    /**
     * Initialize Variables Name list from the summary file.
     */
    Map<String, Name> getCoverages() {
        return coveragesMap;
    }

    Name getCoverageName(String varName) {
        Map<String, Name> coveragesMap = getCoverages();
        if (coveragesMap.containsKey(varName)) {
            return coveragesMap.get(varName);
        }
        return null;
    }

    SimpleFeatureType initializeSchema(Map<String, Serializable> params, final String schemaTypeName) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String schemaType = schemaMapping.get(schemaTypeName);
        // for the moment we only handle data in 4326
        CoordinateReferenceSystem actualCRS = DefaultGeographicCRS.WGS84;
        SimpleFeatureType indexSchema = null;
        initializeParams(params);

        if (schemaType == null && schemaTypeName.equalsIgnoreCase(DEFAULT)) {
            schemaMapping.put(schemaTypeName, CoverageSlice.Attributes.FULLSCHEMA);
            schemaType = CoverageSlice.Attributes.FULLSCHEMA;
        }
        String schema = schemaType;
        if (schema != null) {
            schema = schema.trim();
            // get the schema
            try {
                indexSchema = DataUtilities.createType(schemaTypeName, schema);
                indexSchema = DataUtilities.createSubType(indexSchema,
                        DataUtilities.attributeNames(indexSchema), actualCRS);
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                indexSchema = null;
            }
        }
        return indexSchema;
    }

    void initializeParams(Map<String, Serializable> params) throws IOException {
        final String filePath = slicesIndexFile.getAbsolutePath();
        datastoreProps = new DatastoreProperties(FilenameUtils.removeExtension(FilenameUtils.getName(filePath)).replace(".", ""));
        
        try {
            // create a datastore as instructed
            params.putAll(Utilities.createDataStoreParamsFromProperties(datastoreProps,  DatastoreProperties.SPI));
    
            // set ParentLocation parameter since for embedded database like H2 we must change the database
            // to incorporate the path where to write the db
            params.put("ParentLocation", DataUtilities.fileToURL(destinationDir).toExternalForm());
//            String typeName = datastoreProps.getProperty("database");
//            if (typeName != null) {
//                params.put(DatastoreProperties.TYPE_NAME, typeName);
//            }

        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    /**
     * Dispose the Manager
     */
    public void dispose() {
        try {
            if (slicesIndexList != null) {
                slicesIndexList = null;
            }
            
            if (slicesIndexManager != null) {
                slicesIndexManager.dispose();
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Errors Disposing the indexer." + e.getLocalizedMessage());
            }
        } finally {
            slicesIndexManager = null;
            slicesIndexList = null;
        }
    }

    /**
     * Return a {@link UnidataSlice2DIndex} related to the provided imageIndex
     * @param imageIndex
     * @return
     * @throws IOException
     */
    public UnidataSlice2DIndex getSlice2DIndex(final int imageIndex) throws IOException {
        UnidataSlice2DIndex variableIndex;
        if (slicesIndexManager != null) {
            variableIndex = slicesIndexManager.getSlice2DIndex(imageIndex);
        } else {
            variableIndex = slicesIndexList.get(imageIndex);
        }
        return variableIndex;
    }

    public File getSlicesIndexFile() {
        return slicesIndexFile;
    }

    public File getIndexerFile() {
        return indexerFile;
    }

    public void addVariable(final UnidataSlice2DIndex variableIndex) {
        slicesIndexList.add(variableIndex);
    }

    public void addCoverage(String varName, Name coverageName) {
        if (coveragesMap == null) {
            coveragesMap = new HashMap<String, Name>();
            variablesMap = new HashMap<Name, String>();
        }
        coveragesMap.put(varName, coverageName);
        variablesMap.put(coverageName, varName);
        
    }

    public void initSliceManager() throws IOException {
        slicesIndexManager = new UnidataSlice2DIndexManager(slicesIndexFile);
        slicesIndexManager.open();
    }

    public void resetSliceManager() throws IOException {
        if (slicesIndexManager != null) {
            slicesIndexManager.dispose();
        }
        // clean existing index
        slicesIndexList = new ArrayList<UnidataSlice2DIndex>();
    }

    public List<Name> getCoveragesNames() {
        return new ArrayList<Name>(getCoverages().values());
    }

    /**
     * Retrieve basic indexer properties by scanning the indexer XML instance.
     * @throws JAXBException
     */
    public void initIndexer() throws JAXBException {
        if (indexerFile.exists() && indexerFile.canRead()) {
            if (UNMARSHALLER != null) {
                indexer = (Indexer) UNMARSHALLER.unmarshal(indexerFile);
                
                // Parsing schemas
                final SchemasType schemas = indexer.getSchemas();
                if (schemas != null) {
                    List<SchemaType> schemaElements = schemas.getSchema();
                    for (SchemaType schemaElement: schemaElements) {
                        schemaMapping.put(schemaElement.getName(), schemaElement.getAttributes());
                    }
                }

                // Parsing properties collectors
                initPropertiesCollectors();

                // Parsing coverages 
                initCoverages();
                
            }
        }
    }

    /**
     * Init the coverages naming and schema mappings
     */
    private void initCoverages() {
        final Coverages coverages = indexer.getCoverages();
        if (coverages != null) {
            final List<Coverage> coverageElements = coverages.getCoverage();

            // Loop over coverages
            for (Coverage coverageElement : coverageElements) {
                final SchemaType coverageSchema = coverageElement.getSchema();
                String coverageName = coverageElement.getName();
                if (coverageName == null) {
                    // null coverageName... try to setup it through name collector
                    coverageName = getCoverageNameFromCollector(coverageElement.getNameCollector());
                }

                String schemaName = coverageName;

                // in case of coverageSchemaRef not null, link to that reference schema
                final String coverageSchemaRef = coverageSchema.getRef();
                if (coverageSchemaRef == null || coverageSchemaRef.trim().length() == 0)  {
                    schemaMapping.put(coverageName, coverageSchema.getAttributes());
                } else {
                    schemaName = coverageSchemaRef;
                }

                final Name name = new NameImpl(coverageName);
                coveragesToSchemaMap.put(coverageName, schemaName);

                String origName = coverageElement.getOrigName();
                if (origName != null && !origName.isEmpty()) {
                    origName = origName.trim();
                } else {
                    origName = coverageName;
                }
                addCoverage(origName, name);
            }
        }
    }

    /**
     * Get the coverageName using the specified nameCollector
     * 
     * @param nameCollector The name of the propertiesCollector which will be used to setup 
     * the coverage name
     * @return
     */
    private String getCoverageNameFromCollector(final String nameCollector) {
        String coverageName = null;
        if (collectors != null && collectors.containsKey(nameCollector)) {
            Map<String, Object> keyValues = new HashMap<String, Object>();
            PropertiesCollector collector = collectors.get(nameCollector);
            collector.collect(mainFile);
            collector.setProperties(keyValues);
            collector.reset();
            coverageName = (String) keyValues.get(COVERAGE_NAME);
        }
        return coverageName;
    }

    /**
     * Initialize the propertiesCollectors machinery
     */
    private void initPropertiesCollectors() {
        final Collectors collectors = indexer.getCollectors();
        if (collectors != null) {
            List<Collector> collectorList = collectors.getCollector();
            if (collectorList != null) {
                this.collectors = new HashMap<String, PropertiesCollector>();
                for (Collector collector: collectorList) {
                    final String collectorName = collector.getName();
                    final String spiName = collector.getSpi();
                    final String value = collector.getValue();
                    final String mapped = collector.getMapped();
                    PropertiesCollectorSPI selectedSPI = null;
                    for (PropertiesCollectorSPI spi : pcSPIs) {
                        if (spi.isAvailable() && spi.getName().equalsIgnoreCase(spiName)) {
                            selectedSPI = spi;
                            break;
                        }
                    }
                    if (selectedSPI == null) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.info("Unable to find a PropertyCollector for this SPI: " + spiName);
                        }
                        continue;
                    }

                    // property names
                    final String propertyNames[] = new String[]{mapped != null ? mapped : COVERAGE_NAME};

                    // create the PropertiesCollector
                    final PropertiesCollector pc = selectedSPI.create(DefaultPropertiesCollectorSPI.REGEX_PREFIX + value,  Arrays.asList(propertyNames));
                    if (pc != null) {
                        this.collectors.put(collectorName, pc);
                    }
                }
            }
        }
    }

    public String getTypeName(String coverageName) {
        return coveragesToSchemaMap.get(coverageName);
    }
}