/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    http://java.net/projects/imageio-ext/
 *    (C) 2007 - 2009, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.imageio.unidata;

import it.geosolutions.imageio.stream.input.URIImageInputStream;
import it.geosolutions.imageio.utilities.ImageIOUtilities;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.coverage.io.CoverageSourceDescriptor;
import org.geotools.coverage.io.catalog.CoverageSlice;
import org.geotools.coverage.io.util.Utilities;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.imageio.GeoSpatialImageReader;
import org.geotools.imageio.unidata.UnidataUtilities.CheckType;
import org.geotools.imageio.unidata.UnidataUtilities.KeyValuePair;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An abstract class that handles most of the ucar netcdf libs backed datatypes.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 * @author Andrea Antonello
 * 
 * TODO caching for {@link CoverageSourceDescriptor}
 */
public abstract class UnidataImageReader extends GeoSpatialImageReader {

    private final static Logger LOGGER = Logging.getLogger(UnidataImageReader.class.toString());

    protected static class VariableWrapper {

        private Variable variable;

        private String name;

        private int width;

        private int height;

        private int tileHeight;

        private int tileWidth;

        private int rank;

        private int numBands;

        private SampleModel sampleModel;

        public void setSampleModel(SampleModel sampleModel) {
            this.sampleModel = sampleModel;
        }

        public VariableWrapper(Variable variable) {
            this.variable = variable;
            rank = variable.getRank();
            width = variable.getDimension(rank - UnidataUtilities.X_DIMENSION).getLength();
            height = variable.getDimension(rank - UnidataUtilities.Y_DIMENSION).getLength();
            numBands = rank > 2 ? variable.getDimension(2).getLength() : 1;
            tileHeight = height;
            tileWidth = width;
            name = variable.getFullName();
            final int bufferType = UnidataUtilities.getRawDataType(variable);
            sampleModel = new BandedSampleModel(bufferType, getWidth(), getHeight(), 1);
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public SampleModel getSampleModel() {
            return sampleModel;
        }

        public int getNumBands() {
            return numBands;
        }

        public int getTileHeight() {
            return tileHeight;
        }

        public int getTileWidth() {
            return tileWidth;
        }

        public void setTileHeight(int tileHeight) {
            this.tileHeight = tileHeight;
        }

        public void setTileWidth(int tileWidth) {
            this.tileWidth = tileWidth;
        }

        public Variable getVariable() {
            return variable;
        }

        /**
         * @return the number of dimensions in the variable.
         */
        public int getRank() {
            return rank;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * "Hard coded" properties to setup an h2 db containing slices index
     * TODO: consider moving that DB on a hidden folder such as /.indexes
     * together with the other indexes files (variable index and coverage summary)
     *
     */
    static class DatastoreProperties extends Properties{
        /** serialVersionUID */
        private static final long serialVersionUID = -7633208082806229874L;
        
        final static String USER = "geotools";
        final static String PASSWORD = "geotools";
        final static String DRIVER = "org.h2.Driver";
        final static String TYPE = "javax.sql.DataSource";
        final static H2DataStoreFactory SPI = new H2DataStoreFactory();
        
        final static String URL_PREFIX = "jdbc:h2:";
        String database = null;
        String typeName = null;
        final static String TYPE_NAME = "TypeName";
        
        public DatastoreProperties(String database) {
            super();
            setProperty("user", USER);
            setProperty("passwd", PASSWORD);
            setProperty("driver", DRIVER);
            setProperty("type", TYPE);
            setProperty("urlPrefix", URL_PREFIX + database);
            setProperty("database", database);            
            setProperty(TYPE_NAME, database);
        }
    }
    
    protected Properties datastoreProps = null;
    
    /** Summary set of coverage names */
    protected List<Name> coverages = new ArrayList<Name>();
    
    @Override
    public List<Name> getNames() {
        return Collections.unmodifiableList(coverages);
    }

    @Override
    public int getCoveragesNumber() {
        return coverages.size();
    }

    /**
     * The NetCDF dataset, or {@code null} if not yet open. The NetCDF file is
     * open by {@link #ensureOpen} when first needed.
     */
    protected NetcdfDataset dataset;
    
    /** File storing the slices index (index, Tsection, Zsection) */
    protected File slicesIndexFile;
    
    /** File storing the coverage names summary. This will allow knowing the available 
     * coverages name without opening and scanning again all the dataset */ 
    protected File variablesSummaryFile;
    protected List<UnidataSlice2DIndex> slicesIndexList;
    protected UnidataSlice2DIndexManager slicesIndexManager;
    
    protected Map<String, Variable> coordinatesVariables;
    protected CheckType checkType = CheckType.UNSET;

    /** The source file */
    protected File file;

    /** The parent dir of the source file */
    protected File parentDir;

    public UnidataImageReader( ImageReaderSpi originatingProvider ) {
        super(originatingProvider);
    }

    /**
     * Get the {@link NetcdfDataset} out og an input object.
     * 
     * @param input the input object.
     * @return the dataset or <code>null</code>.
     * @throws IOException
     */
    private NetcdfDataset extractDataset( Object input ) throws IOException {
        NetcdfDataset dataset = null;
        if (input instanceof URIImageInputStream) {
            URIImageInputStream uriInStream = (URIImageInputStream) input;
            dataset = NetcdfDataset.openDataset(uriInStream.getUri().toString());
        }
        if (input instanceof URL) {
            final URL tempURL = (URL) input;
            String protocol = tempURL.getProtocol();
            if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("dods")) {
                dataset = NetcdfDataset.openDataset(tempURL.toExternalForm());
            }
        }

        if (dataset == null) {
            dataset = UnidataUtilities.getDataset(input);
        }

        return dataset;
    }
    
    protected void setNumImages( final int numImages ) {
        if (numImages <= 0) {
            throw new IllegalArgumentException("Number of Images is negative: " + numImages);
        }
        if (this.numImages == -1) {
            this.numImages = numImages;
        }
    }

    @Override
    public int getHeight( int imageIndex ) throws IOException {
        final VariableWrapper wrapper = getVariableWrapper(imageIndex);
        if (wrapper != null){
        	return wrapper.getHeight();
        }
        return -1;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes( int imageIndex ) throws IOException {
        final List<ImageTypeSpecifier> l = new java.util.ArrayList<ImageTypeSpecifier>();
        final VariableWrapper wrapper = getVariableWrapper(imageIndex);
        if (wrapper != null) {
            final SampleModel sampleModel = wrapper.getSampleModel();
            final ImageTypeSpecifier imageType = new ImageTypeSpecifier(ImageIOUtilities.createColorModel(sampleModel),
                    sampleModel);
            l.add(imageType);
        }
        return l.iterator();
    }

    public int getWidth( int imageIndex ) throws IOException {
        final VariableWrapper wrapper = getVariableWrapper(imageIndex);
        if (wrapper != null){
        	return wrapper.getWidth();
        }
        return -1;
    }

    /**
     * Get an {@link Attribute} value as string by name and image index.
     * 
     * @param imageIndex the image index.
     * @param attributeName the name of the {@link Attribute}.
     * @return the attribute value as string.
     */
    public String getAttributeAsString( final int imageIndex, final String attributeName ) {
        return getAttributeAsString(imageIndex, attributeName, false);
    }

    /**
     * Get an {@link Attribute} value as string by name and image index.
     * 
     * @param imageIndex the image index.
     * @param attributeName the name of the {@link Attribute}.
     * @param isUnsigned a flag that allows to handle byte
     *                      attributes as unsigned.
     * @return the attribute value as string.
     */
    public String getAttributeAsString( final int imageIndex, final String attributeName, final boolean isUnsigned ) {
        String attributeValue = "";
        final VariableWrapper wrapper = getVariableWrapper(imageIndex);
        final Attribute attr = wrapper.getVariable().findAttributeIgnoreCase(attributeName);
        if (attr != null){
        	attributeValue = UnidataUtilities.getAttributesAsString(attr, isUnsigned);
        }
        return attributeValue;
    }

    /**
     * Get an {@link Attribute} as a {@link KeyValuePair} representation as name/value. 
     * 
     * @param imageIndex the image index.
     * @param attributeIndex the attribute index.
     * @return the key/value pair of the attribute.
     * @throws IOException
     */
    public KeyValuePair getAttribute( final int imageIndex, final int attributeIndex ) throws IOException {
        KeyValuePair attributePair = null;
        final Variable var = getVariable(imageIndex);
        if (var != null){
        	attributePair = UnidataUtilities.getAttribute(var, attributeIndex);
        }
        return attributePair;
    }

    /**
     * Get a {@link Variable} by image index.
     * 
     * @param imageIndex the image index.
     * @return the {@link Variable}.
     */
    public Variable getVariable( final int imageIndex ) {
        Variable var = null;
        final VariableWrapper wrapper = getVariableWrapper(imageIndex);
        if (wrapper != null)
            var = wrapper.getVariable();
        return var;
    }

    /**
     * Get a {@link Variable} by name.
     * 
     * @param varName the name of the {@link Variable} to pick.
     * @return the variable or <code>null</code>.
     */
    public Variable getVariableByName( final String varName ) {
        final List<Variable> varList = dataset.getVariables();
        for( Variable var : varList ) {
            if (var.getFullName().equals(varName))
                return var;
        }
        return null;
    }

    /**
     * Get the number of available attributes of a variable identified by the image index.
     * 
     * @param imageIndex the image index.
     * @return the number of {@link Attribute}s.
     */
    public int getNumAttributes( int imageIndex ) {
        int numAttribs = 0;
        final Variable var = getVariable(imageIndex);
        if (var != null) {
            final List<Attribute> attributes = var.getAttributes();
            if (attributes != null && !attributes.isEmpty())
                numAttribs = attributes.size();
        }
        return numAttribs;
    }

    /**
     * Get a Global{@link Attribute} as a {@link KeyValuePair} representation as name/value
     * by index. 
     * 
     * @param attributeIndex the attribute index.
     * @return the global attribute identified by the index.
     * @throws IOException
     */
    public KeyValuePair getGlobalAttribute( final int attributeIndex ) throws IOException {
        return UnidataUtilities.getGlobalAttribute(dataset, attributeIndex);
    }

    /**
     * Retrieve the scale factor for the specified imageIndex. Return
     * {@code Double.NaN} if parameter isn't available
     * 
     * @throws IOException
     */
    public double getScale( final int imageIndex ) throws IOException {
        checkImageIndex(imageIndex);
        double scale = Double.NaN;
        final String scaleS = getAttributeAsString(imageIndex, UnidataUtilities.DatasetAttribs.SCALE_FACTOR);
        if (scaleS != null && scaleS.trim().length() > 0)
            scale = Double.parseDouble(scaleS);
        return scale;
    }

    /**
     * Retrieve the fill value for the specified imageIndex. Return
     * {@code Double.NaN} if parameter isn't available
     * 
     * @throws IOException
     */
    public double getFillValue( final int imageIndex ) throws IOException {
        checkImageIndex(imageIndex);
        double fillValue = Double.NaN;
        final String fillValueS = getAttributeAsString(imageIndex, UnidataUtilities.DatasetAttribs.FILL_VALUE);
        if (fillValueS != null && fillValueS.trim().length() > 0)
            fillValue = Double.parseDouble(fillValueS);
        return fillValue;
    }

    /**
     * Retrieve the offset factor for the specified imageIndex. Return
     * {@code Double.NaN} if parameter isn't available
     * 
     * @throws IOException
     */
    public double getOffset( final int imageIndex ) throws IOException {
        checkImageIndex(imageIndex);
        double offset = Double.NaN;
        final String offsetS = getAttributeAsString(imageIndex, UnidataUtilities.DatasetAttribs.ADD_OFFSET);
        if (offsetS != null && offsetS.trim().length() > 0)
            offset = Double.parseDouble(offsetS);
        return offset;
    }

    /**
     * Retrieve the valid Range for the specified imageIndex. Return null if
     * parameters aren't available
     * 
     * @throws IOException
     */
    public double[] getValidRange( final int imageIndex ) throws IOException {
        checkImageIndex(imageIndex);
        double range[] = null;

        final String validRange = getAttributeAsString(imageIndex, UnidataUtilities.DatasetAttribs.VALID_RANGE, true);
        if (validRange != null && validRange.trim().length() > 0) {
            String validRanges[] = validRange.split(",");
            if (validRanges.length == 2) {
                range = new double[2];
                range[0] = Double.parseDouble(validRanges[0]);
                range[1] = Double.parseDouble(validRanges[1]);
            }
        } else {
            final String validMin = getAttributeAsString(imageIndex, UnidataUtilities.DatasetAttribs.VALID_MIN, true);
            final String validMax = getAttributeAsString(imageIndex, UnidataUtilities.DatasetAttribs.VALID_MAX, true);
            if (validMax != null && validMax.trim().length() > 0 && validMin != null && validMin.trim().length() > 0) {
                range = new double[2];
                range[0] = Double.parseDouble(validMin);
                range[1] = Double.parseDouble(validMax);
            }
        }
        return range;
    }

    /**
     * Reset the status of this reader
     */
    public void reset() {
        super.setInput(null, false, false);
        dispose();
    }

    @Override
    public void setInput( Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        if (dataset != null) {
            reset();
        }
        try {
            dataset = extractDataset(input);

            file = UnidataUtilities.getFile(input);
            final String parent = file.getParent();
            parentDir = new File(parent);
            if (file != null) {
                slicesIndexFile = new File(file.getAbsolutePath() + ".idx");
                variablesSummaryFile = new File(file.getAbsolutePath() + ".cvs");
            }

            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Index Initialization. Scan the coverageDescriptorsCache and store indexing information.
     * @param coverageDescriptorsCache
     * @return
     * @throws InvalidRangeException
     * @throws IOException
     */
    protected int initIndex( final List<Variable> variables ) throws InvalidRangeException, IOException {
        DefaultTransaction transaction = new DefaultTransaction("indexTransaction" + System.nanoTime());
        final Map<String, Serializable> params = new HashMap<String, Serializable>();
        boolean rollback = false;
        
        try {
            
            // create the schema for the slices index
            SimpleFeatureType indexSchema = initializeSchema(params);
            initCatalog(params, true, DatastoreProperties.SPI, indexSchema);
            int numImages = 0;
            if (variables != null) {
                for (final Variable variable : variables) {
                    if (variable != null && variable instanceof VariableDS) {
                        String varName = variable.getFullName();
                        if (!UnidataUtilities.isVariableAccepted(variable, checkType)) {
                            continue;
                        }

                        CoordinateSystem cs = UnidataCRSUtilities.getCoordinateSystem((VariableDS) variable);
                        // Add the accepted variable to the list of coverages name
                        coverages.add(new NameImpl(varName));
                        
                        // Currently, we only support Geographic CRS
                        Geometry geometry = UnidataCRSUtilities.extractEnvelopeAsGeometry((VariableDS)variable);

                        int variableImageStartIndex = numImages;
                        int variableImageNum = 0;
                        Range variableRange = null;
                        // get the length of the coverageDescriptorsCache in each dimension
                        int[] shape = variable.getShape();
                        switch (shape.length) {
                        case 2:
                            variableImageNum = numImages + 1;
                            variableRange = new Range(numImages, variableImageNum);
                            numImages++;
                            break;
                        case 3:
                            variableImageNum = numImages + shape[0];
                            variableRange = new Range(numImages, variableImageNum);
                            numImages += shape[0];
                            break;
                        case 4:
                            variableImageNum = numImages + shape[0] * shape[1];
                            variableRange = new Range(numImages, variableImageNum);
                            numImages += shape[0] * shape[1];
                            break;
                        default:
                            if (LOGGER.isLoggable(Level.WARNING))
                                LOGGER.warning("Ignoring variable: " + varName
                                        + " with shape length: " + shape.length);
                            continue;
                        }

                        /*
                         * extract also index information
                         */
                        final boolean hasVerticalAxis = cs.hasVerticalAxis();
                        final ListFeatureCollection collection= new ListFeatureCollection(indexSchema);
                        int features = 0;
                        for (int imageIndex = variableImageStartIndex; imageIndex < variableImageNum; imageIndex++) {
                            final int rank = variable.getRank();
                            final int bandDimension = rank - UnidataUtilities.Z_DIMENSION;
                            int zIndex = -1;
                            int tIndex = -1;
                            for (int i = 0; i < rank; i++) {
                                switch (rank - i) {
                                case UnidataUtilities.X_DIMENSION:
                                case UnidataUtilities.Y_DIMENSION:
                                    break;
                                default: {
                                    if (i == bandDimension && hasVerticalAxis) {
                                        zIndex = UnidataUtilities.getZIndex(variable, variableRange, imageIndex);
                                    } else {
                                        tIndex = UnidataUtilities.getTIndex(variable, variableRange, imageIndex);
                                    }
                                    break;
                                }
                                }
                            }
                            
                            //Put a new sliceIndex in the list
                            UnidataSlice2DIndex variableIndex = new UnidataSlice2DIndex(tIndex, zIndex, varName);
                            slicesIndexList.add(variableIndex);

                            // Create a feature for that index to be put in the CoverageSlicesCatalog
                            Date startDate = UnidataUtilities.getTimeValueByIndex(this, variable, tIndex, cs);
                            double verticalValue = UnidataUtilities.getVerticalValueByIndex(this, variable, zIndex, cs);

                            final SimpleFeature feature = DataUtilities.template(indexSchema);
                            feature.setAttribute(CoverageSlice.Attributes.GEOMETRY, geometry);
                            feature.setAttribute(CoverageSlice.Attributes.INDEX, imageIndex);
                            feature.setAttribute(CoverageSlice.Attributes.COVERAGENAME, varName);
                            if (startDate != null) {
                                feature.setAttribute(CoverageSlice.Attributes.TIME, startDate);
                            }
                            if (!Double.isNaN(verticalValue)) {
                                feature.setAttribute(CoverageSlice.Attributes.ELEVATION, verticalValue);
                            }
//                            slicesCatalog.addGranule(feature, transaction);
                            
                            collection.add(feature);
                            features++;
                            
                            if (features % 1000 == 0) {
                                slicesCatalog.addGranules(collection, transaction);
                                collection.clear();
                            }
                        }
                        // add residual features
                        if (collection.size() > 0) {
                            slicesCatalog.addGranules(collection, transaction);
                            collection.clear();
                        }
                    }
                }
            }
            
            // Write collected information 
            UnidataSlice2DIndexManager.writeIndexFile(slicesIndexFile, slicesIndexList);
            writeVariablesSummary(variablesSummaryFile, coverages);
        } catch (Throwable e) {
            rollback = true;
            throw new IOException(e);
        } finally {
            if (rollback) {
                transaction.rollback();
            } else {
                transaction.commit();
            }
            try {
                transaction.close();
            } catch (Throwable t) {
                
            }
        }
        return numImages;
    }

    /**
     * Initialize Variables Name list from the summary file.
     */
    private List<Name> initVariablesNames() {
        BufferedReader reader = null;
        List<Name> coverages = new LinkedList<Name>();
        try {
            reader = new BufferedReader(new FileReader(variablesSummaryFile));
            String line = null;
            while ((line = reader.readLine()) != null) {
                coverages.add(new NameImpl(line));
            }
        } catch (FileNotFoundException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "file not found" + e);
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "unable to read from the coverages summary file " + e);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable t) {
                    
                }
            }
        }
        return coverages;
    }

    /**
     * Write to disk the variable summary, a simple text file containing variable names.
     * @param variablesSummaryFile
     * @param coverages
     */
    private void writeVariablesSummary(final File variablesSummaryFile, final List<Name> coverages) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(variablesSummaryFile));
            for (Name name : coverages) {
                writer.write(name.toString() + "\n");
            }
        } catch (FileNotFoundException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "file not found" + e);
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "unable to write the coverages summary file " + e);
            }
        } finally {
            if (writer != null) {
                IOUtils.closeQuietly(writer);
            }
        }
    }

    private SimpleFeatureType initializeSchema(Map<String, Serializable> params) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        
        CoordinateReferenceSystem actualCRS = null;
        SimpleFeatureType indexSchema = null;
//        final File datastoreProperties= new File(slicesIndexFile.getParent(),"datastore.properties");
//        if (Utilities.checkFileReadable(datastoreProperties)) {
            // read the properties file
//        
//                Properties datastoreProps = Utilities.loadPropertiesFromURL(DataUtilities.fileToURL(datastoreProperties));
//                if (datastoreProps == null) {
//                    throw new IOException("Unable to load properties from:"
//                            + datastoreProperties.getAbsolutePath());
//                }
//        final String SPIClass = datastoreProps.getProperty(DatastoreProperties.SPI);
//        slicesIndexDatastore = new File(datastoreProps.getProperty("Dat))
        // SPI
        
        initializeParams(params);

        String schema = CoverageSlice.Attributes.FULLSCHEMA;
        if (schema != null) {
            schema = schema.trim();
            // get the schema
            try {
                indexSchema = DataUtilities.createType((String)datastoreProps.get(DatastoreProperties.TYPE_NAME), schema);
                // override the crs in case the provided one was wrong or absent
                indexSchema = DataUtilities.createSubType(indexSchema,
                        DataUtilities.attributeNames(indexSchema), actualCRS);
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                indexSchema = null;
            }
        }
        // }
        return indexSchema;
    }

    private void initializeParams(Map<String, Serializable> params) throws IOException {
        final String filePath = slicesIndexFile.getAbsolutePath();
        datastoreProps = new DatastoreProperties(FilenameUtils.removeExtension(FilenameUtils.getBaseName(filePath)));
        
        try {
            // create a datastore as instructed
            params.putAll(Utilities.createDataStoreParamsFromProperties(datastoreProps,  DatastoreProperties.SPI));
    
            // set ParentLocation parameter since for embedded database like H2 we must change the database
            // to incorporate the path where to write the db
            params.put("ParentLocation", DataUtilities.fileToURL(parentDir).toExternalForm());
            String typeName = datastoreProps.getProperty("database");
            if (typeName != null) {
                params.put(DatastoreProperties.TYPE_NAME, typeName);
            }

        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    protected void fillCoordinatesMap( final List<Variable> variables ) throws IOException {
        for( Variable variable : variables ) {
            String varName = variable.getFullName();
            if (variable.isCoordinateVariable()) {
                coordinatesVariables.put(varName, variable);
            }
            if (variable instanceof CoordinateAxis1D) {

                // FIXME check if this is still a problem
                // Due to a netCDF library bug, coordinates values need to
                // be read to be properly obtained afterwards.
                // Otherwise it may return NaN in some coords.
                if (varName.equalsIgnoreCase(UnidataUtilities.LAT) || varName.equalsIgnoreCase(UnidataUtilities.LATITUDE)
                        || varName.equalsIgnoreCase(UnidataUtilities.LON) || varName.equalsIgnoreCase(UnidataUtilities.LONGITUDE)) {
                    variable.read();
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        checkType = CheckType.UNSET;

        coordinatesVariables.clear();
        coordinatesVariables = null;
        if (slicesIndexList != null) {
            slicesIndexList = null;
        }

        numImages = -1;
        try {
            if (dataset != null) {
                dataset.close();
            }
            if (slicesIndexManager != null) {
                slicesIndexManager.dispose();
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.warning("Errors closing NetCDF dataset." + e.getLocalizedMessage());
        } finally {
            dataset = null;
            slicesIndexManager = null;
        }
    }

    /**
     * Initialize main properties for this reader.
     * 
     * @throws exception
     *                 {@link InvalidRangeException}
     */
    private synchronized void init() throws IOException {
        int numImages = 0;
        slicesIndexList = new ArrayList<UnidataSlice2DIndex>();

        // variablesMap = new HashMap<Range, NetCDFVariableWrapper>();
        coordinatesVariables = new HashMap<String, Variable>();

        try {
            if (dataset != null) {
                checkType = UnidataUtilities.getCheckType(dataset);

                final List<Variable> variables = dataset.getVariables();
                fillCoordinatesMap(variables);

                if (slicesIndexFile != null) {
                    // === use sidecar index
                    if (slicesIndexFile.exists()) {
                        slicesIndexManager = new UnidataSlice2DIndexManager(slicesIndexFile);
                        slicesIndexManager.open();
                        numImages = slicesIndexManager.getNumberOfRecords();
                        if (!ignoreMetadata) {
                            final Map<String, Serializable> params = new HashMap<String, Serializable>();
                            initializeParams(params);
                            initCatalog(params, false, DatastoreProperties.SPI, null);
                            coverages = initVariablesNames();
                        }
                    }

                    if (numImages <= 0 || !slicesIndexFile.exists()) {
                        // === index doesn't exists already, build it first
                        // close existing
                        if (slicesIndexManager != null) {
                            slicesIndexManager.dispose();
                        }
                        numImages = initIndex(variables);

                        // clean existing index
                        slicesIndexList = null;

                        // reopen file to cut caching
                        slicesIndexManager = new UnidataSlice2DIndexManager(slicesIndexFile);
                        slicesIndexManager.open();
                        numImages = slicesIndexManager.getNumberOfRecords();
                    }

                } else {
                    // === the dataset is no file dataset, need to build memory index
                    numImages = initIndex(variables);
                }

            } else{
                throw new IllegalArgumentException("Not a valid dataset has been found");
            }
        } catch (InvalidRangeException e) {
            throw new IllegalArgumentException("Error occurred during NetCDF file parsing", e);
        }
        setNumImages(numImages);
    }

//    /**
//     * Invoked by the NetCDF library when an error occurred during the read
//     * operation. Users should not invoke this method directly.
//     */
//    public void setError( final String message ) {
//        lastError = message;
//    }
    
    /**
     * Wraps a generic exception into a {@link IIOException}.
     */
    protected IIOException netcdfFailure( final Exception e ) throws IOException {
        return new IIOException(new StringBuilder("Can't read file ").append(dataset.getLocation()).toString(), e);
    }

    protected VariableWrapper createVariableWrapper( Variable variable ){
        return new VariableWrapper(variable);
    }

    /**
     * Return the {@link UnidataSlice2DIndex} associated to the specified imageIndex
     * @param imageIndex
     * @return
     * @throws IOException
     */
    public UnidataSlice2DIndex getSlice2DIndex( int imageIndex ) throws IOException {
        UnidataSlice2DIndex variableIndex;
        if (slicesIndexManager != null) {
            variableIndex = slicesIndexManager.getSlice2DIndex(imageIndex);
        } else {
            variableIndex = slicesIndexList.get(imageIndex);
        }
        return variableIndex;
    }

    /**
     * Return a {@link Variable} coordinate with the specified name
     * @param coordName
     * @return
     */
    public Variable getCoordinate( final String coordName ) {
        if (coordName != null && coordName.length() > 0 && coordinatesVariables.containsKey(coordName)) {
            return coordinatesVariables.get(coordName);
        }
        return null;
    }

    /**
     * Return the {@link VariableWrapper} related to that imageIndex
     * @param imageIndex
     * @return
     */
    protected VariableWrapper getVariableWrapper( int imageIndex ) {
        checkImageIndex(imageIndex);
        try {
            UnidataSlice2DIndex slice2DIndex = getSlice2DIndex(imageIndex);
            if (slice2DIndex != null) {
                Variable variable = dataset.findVariable(slice2DIndex.getVariableName());
                if (variable != null) {
                    return createVariableWrapper(variable);
                }
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
        }
        return null;
    }
    

    @Override
    public CoverageSourceDescriptor createCoverageDescriptor(Name name) {
        return new UnidataCoverageDescriptor(this,(VariableDS) getVariableByName(name.toString()));
    }

    /**
     * @see javax.imageio.ImageReader#read(int, javax.imageio.ImageReadParam)
     */
    @Override
    public BufferedImage read( int imageIndex, ImageReadParam param ) throws IOException {
        clearAbortRequest();
    
        final UnidataSlice2DIndex slice2DIndex = getSlice2DIndex(imageIndex);
        final Variable variable = dataset.findVariable(slice2DIndex.getVariableName());
        final VariableWrapper wrapper = new VariableWrapper(variable);
    
        /*
         * Fetches the parameters that are not already processed by utility
         * methods like 'getDestination' or 'computeRegions' (invoked below).
         */
        final int strideX, strideY;
        // final int[] srcBands;
        final int[] dstBands;
        if (param != null) {
            strideX = param.getSourceXSubsampling();
            strideY = param.getSourceYSubsampling();
            // srcBands = param.getSourceBands();
            dstBands = param.getDestinationBands();
        } else {
            strideX = 1;
            strideY = 1;
            // srcBands = null;
            dstBands = null;
        }
    
        /*
         * Gets the destination image of appropriate size. We create it now
         * since it is a convenient way to get the number of destination bands.
         */
        final int width = wrapper.getWidth();
        final int height = wrapper.getHeight();
        /*
         * Computes the source region (in the NetCDF file) and the destination
         * region (in the buffered image). Copies those informations into UCAR
         * Range structure.
         */
        final Rectangle srcRegion = new Rectangle();
        final Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, null, srcRegion, destRegion);
         flipVertically(param, height, srcRegion);
        int destWidth = destRegion.x + destRegion.width;
        int destHeight = destRegion.y + destRegion.height;
    
        /*
         * build the ranges that need to be read from each 
         * dimension based on the source region
         */
        final List<Range> ranges = new LinkedList<Range>();
        try {
            // add the ranges the COARDS way: T, Z, Y, X
            // T
            int first = slice2DIndex.getTIndex();
            int length = 1;
            int stride = 1;
            if (first != -1){
                ranges.add(new Range(first, first + length - 1, stride));
            }
            // Z
            first = slice2DIndex.getZIndex();
            if (first != -1){
                ranges.add(new Range(first, first + length - 1, stride));
            }
            // Y
            first = srcRegion.y;
            length = srcRegion.height;
            stride = strideY;
            ranges.add(new Range(first, first + length - 1, stride));
            // X
            first = srcRegion.x;
            length = srcRegion.width;
            stride = strideX;
            ranges.add(new Range(first, first + length - 1, stride));
        } catch (InvalidRangeException e) {
            throw netcdfFailure(e);
        }
    
        /*
         * create the section of multidimensional array indices
         * that defines the exact data that need to be read 
         * for this image index and parameters 
         */
        final Section section = new Section(ranges);
    
        /*
         * Setting SampleModel and ColorModel.
         */
        final SampleModel sampleModel = wrapper.getSampleModel().createCompatibleSampleModel(destWidth, destHeight);
        final ColorModel colorModel = ImageIOUtilities.createColorModel(sampleModel);
    
        final WritableRaster raster = Raster.createWritableRaster(sampleModel, new Point(0, 0));
        final BufferedImage image = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
    
        /*
         * Reads the requested sub-region only.
         */
        processImageStarted(imageIndex);
        final int numDstBands = 1;
        final float toPercent = 100f / numDstBands;
        final int type = raster.getSampleModel().getDataType();
        final int xmin = destRegion.x;
        final int ymin = destRegion.y;
        final int xmax = destRegion.width + xmin;
        final int ymax = destRegion.height + ymin;
        for( int zi = 0; zi < numDstBands; zi++ ) {
            // final int srcBand = (srcBands == null) ? zi : srcBands[zi];
            final int dstBand = (dstBands == null) ? zi : dstBands[zi];
            final Array array;
            try {
                array = variable.read(section);
            } catch (InvalidRangeException e) {
                throw netcdfFailure(e);
            }
            final IndexIterator it = array.getIndexIterator();
             for (int y = ymax; --y >= ymin;) {
//            for( int y = ymin; y < ymax; y++ ) {
                for( int x = xmin; x < xmax; x++ ) {
                    switch( type ) {
                        case DataBuffer.TYPE_DOUBLE: {
                            raster.setSample(x, y, dstBand, it.getDoubleNext());
                            break;
                        }
                        case DataBuffer.TYPE_FLOAT: {
                            raster.setSample(x, y, dstBand, it.getFloatNext());
                            break;
                        }
                        case DataBuffer.TYPE_BYTE: {
                            byte b = it.getByteNext();
                            // int myByte = (0x000000FF & ((int) b));
                            // short anUnsignedByte = (short) myByte;
                            // raster.setSample(x, y, dstBand, anUnsignedByte);
                            raster.setSample(x, y, dstBand, b);
                            break;
                        }
                        default: {
                            raster.setSample(x, y, dstBand, it.getIntNext());
                            break;
                        }
                    }
                }
            }
            /*
             * Checks for abort requests after reading. It would be a waste of a
             * potentially good image (maybe the abort request occurred after we
             * just finished the reading) if we didn't implemented the
             * 'isCancel()' method. But because of the later, which is checked
             * by the NetCDF library, we can't assume that the image is
             * complete.
             */
            if (abortRequested()) {
                processReadAborted();
                return image;
            }
            /*
             * Reports progress here, not in the deeper loop, because the costly
             * part is the call to 'variable.read(...)' which can't report
             * progress. The loop that copy pixel values is fast, so reporting
             * progress there would be pointless.
             */
            processImageProgress(zi * toPercent);
        }
        processImageComplete();
        return image;
    }
    
    protected static void flipVertically(final ImageReadParam param, final int srcHeight,
            final Rectangle srcRegion) {
        final int spaceLeft = srcRegion.y;
        srcRegion.y = srcHeight - (srcRegion.y + srcRegion.height);
        /*
         * After the flip performed by the above line, we still have 'spaceLeft' pixels left for a downward translation. We usually don't need to care
         * about if, except if the source region is very close to the bottom of the source image, in which case the correction computed below may be
         * greater than the space left.
         * 
         * We are done if there is no vertical subsampling. But if there is subsampling, then we need an adjustment. The flipping performed above must
         * be computed as if the source region had exactly the size needed for reading nothing more than the last line, i.e. 'srcRegion.height' must
         * be a multiple of 'sourceYSubsampling' plus 1. The "offset" correction is computed below accordingly.
         */
        if (param != null) {
            int offset = (srcRegion.height - 1) % param.getSourceYSubsampling();
            srcRegion.y += offset;
            offset -= spaceLeft;
            if (offset > 0) {
                // Happen only if we are very close to image border and
                // the above translation bring us outside the image area.
                srcRegion.height -= offset;
            }
        }
    }
}