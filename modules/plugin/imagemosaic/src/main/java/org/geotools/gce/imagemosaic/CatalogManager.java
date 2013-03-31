/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.imagemosaic;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.collection.AbstractFeatureVisitor;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalogFactory;
import org.geotools.gce.imagemosaic.catalogbuilder.CatalogBuilderConfiguration;
import org.geotools.gce.imagemosaic.properties.PropertiesCollector;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DefaultProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * An utility class which allows to create schema, catalogs, and populate them
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class CatalogManager {

    private final static PrecisionModel PRECISION_MODEL = new PrecisionModel(PrecisionModel.FLOATING);
    private final static GeometryFactory GEOM_FACTORY = new GeometryFactory(PRECISION_MODEL);;

    /** Default Logger * */
    private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(CatalogManager.class);
    
    /**
     * Create a GranuleCatalog on top of the provided configuration
     * @param runConfiguration
     * @return
     * @throws IOException
     */
    public static GranuleCatalog createCatalog(CatalogBuilderConfiguration runConfiguration) throws IOException {
        //
        // create the index
        //
        // do we have a datastore.properties file?
        final File parent = new File(runConfiguration.getRootMosaicDirectory());
        GranuleCatalog catalog;
        
        // Consider checking that from the indexer if any
        final File datastoreProperties = new File(parent, "datastore.properties");
        // GranuleCatalog catalog = null;
        if (Utils.checkFileReadable(datastoreProperties)) {
            // read the properties file
            Properties properties = Utils.loadPropertiesFromURL(DataUtilities.fileToURL(datastoreProperties));
            if (properties == null) {
                throw new IOException("Unable to load properties from:" + datastoreProperties.getAbsolutePath());
            }
            // SPI
            final String SPIClass = properties.getProperty("SPI");
            try {
                // create a datastore as instructed
                final DataStoreFactorySpi spi = (DataStoreFactorySpi) Class.forName(SPIClass).newInstance();
                final Map<String, Serializable> params = Utils.createDataStoreParamsFromPropertiesFile(properties, spi);

                // set ParentLocation parameter since for embedded database like H2 we must change the database
                // to incorporate the path where to write the db
                params.put("ParentLocation", DataUtilities.fileToURL(parent).toExternalForm());

                catalog = GranuleCatalogFactory.createGranuleCatalog(params, false, true, spi);
            } catch (ClassNotFoundException e) {
                final IOException ioe = new IOException();
                throw (IOException) ioe.initCause(e);
            } catch (InstantiationException e) {
                final IOException ioe = new IOException();
                throw (IOException) ioe.initCause(e);
            } catch (IllegalAccessException e) {
                final IOException ioe = new IOException();
                throw (IOException) ioe.initCause(e);
            }
        } else {

            // we do not have a datastore properties file therefore we continue with a shapefile datastore
            final URL file = new File(parent, runConfiguration.getIndexName() + ".shp").toURI().toURL();
            final Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put(ShapefileDataStoreFactory.URLP.key, file);
            if (file.getProtocol().equalsIgnoreCase("file")) {
                params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
            }
            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.TRUE);
            params.put(ShapefileDataStoreFactory.DBFTIMEZONE.key, TimeZone.getTimeZone("UTC"));
            params.put(Utils.Prop.LOCATION_ATTRIBUTE, runConfiguration.getLocationAttribute());
            catalog = GranuleCatalogFactory.createGranuleCatalog(params, false, true, Utils.SHAPE_SPI);
        }

        return catalog;
    }
    
    /**
     * Create a {@link SimpleFeatureType} from the specified configuration.
     * @param configurationBean
     * @param actualCRS
     * @return
     */
    public static SimpleFeatureType createSchema(CatalogBuilderConfiguration runConfiguration, String name,
            CoordinateReferenceSystem actualCRS) {
        SimpleFeatureType indexSchema = null;
        String schema = runConfiguration.getSchema();
        if (schema != null) {
            schema = schema.trim();
            // get the schema
            try {
                indexSchema = DataUtilities.createType(name, schema);
                // override the crs in case the provided one was wrong or absent
                indexSchema = DataUtilities.createSubType(indexSchema,
                        DataUtilities.attributeNames(indexSchema), actualCRS);
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                indexSchema = null;
            }
        }

        if (indexSchema == null) {
            // Proceed with default Schema
            final SimpleFeatureTypeBuilder featureBuilder = new SimpleFeatureTypeBuilder();
            featureBuilder.setName(runConfiguration.getIndexName());
            featureBuilder.setNamespaceURI("http://www.geo-solutions.it/");
            featureBuilder.add(runConfiguration.getLocationAttribute().trim(), String.class);
            featureBuilder.add("the_geom", Polygon.class, actualCRS);
            featureBuilder.setDefaultGeometry("the_geom");
            String timeAttribute = runConfiguration.getTimeAttribute();
            addAttributes(timeAttribute, featureBuilder, Date.class);
            indexSchema = featureBuilder.buildFeatureType();
        }
        return indexSchema;
    }
    
    /**
     * Add splitted attributes to the featureBuilder
     *
     * @param attribute
     * @param featureBuilder
     * @param classType
     * TODO: Remove that once reworking on the dimension stuff
     */
    private static void addAttributes(String attribute, SimpleFeatureTypeBuilder featureBuilder,
            Class classType) {
        if (attribute != null) {
            if (!attribute.contains(Utils.RANGE_SPLITTER_CHAR)) {
                featureBuilder.add(attribute, classType);
            } else {
                String[] ranges = attribute.split(Utils.RANGE_SPLITTER_CHAR);
                if (ranges.length != 2) {
                    throw new IllegalArgumentException(
                            "All ranges attribute need to be composed of a maximum of 2 elements:\n"
                                    + "As an instance (min;max) or (low;high) or (begin;end) , ...");
                } else {
                    featureBuilder.add(ranges[0], classType);
                    featureBuilder.add(ranges[1], classType);
                }
            }
        }

    }

    /**
     * Get a {@link GranuleSource} related to a specific coverageName from an inputReader
     * and put the related granules into a {@link GranuleStore} related to the same coverageName
     * of the mosaicReader.
     * 
     * @param coverageName the name of the coverage to be managed
     * @param fileBeingProcessed the reference input file
     * @param inputReader the reader source of granules
     * @param mosaicReader the reader where to store source granules
     * @param configuration the configuration
     * @param envelope
     * @param transaction
     * @param propertiesCollectors
     * @throws IOException
     */
    static void updateCatalog(
            final String coverageName,
            final File fileBeingProcessed,
            final AbstractGridCoverage2DReader inputReader,
            final ImageMosaicReader mosaicReader,
            final CatalogBuilderConfiguration configuration, 
            final GeneralEnvelope envelope,
            final DefaultTransaction transaction, 
            final List<PropertiesCollector> propertiesCollectors) throws IOException {
        
        // Retrieving the store and the destination schema
        final GranuleStore store = (GranuleStore) mosaicReader.getGranules(coverageName, false);
        if (store == null) {
            throw new IllegalArgumentException("No valid granule store has been found for: " + coverageName);
        }
        final SimpleFeatureType indexSchema = store.getSchema();
        final SimpleFeature feature = DataUtilities.template(indexSchema);
        store.setTransaction(transaction);
        
        final ListFeatureCollection collection = new ListFeatureCollection(indexSchema);
        final String fileLocation = prepareLocation(configuration, fileBeingProcessed);

        // getting input granules
        if (inputReader instanceof StructuredGridCoverage2DReader) {
            
            //
            // Case A: input reader is a StructuredGridCoverage2DReader. We can get granules from a source 
            // 
            // Getting granule source and its input granules
            final GranuleSource source = ((StructuredGridCoverage2DReader) inputReader).getGranules(coverageName, true);
            final SimpleFeatureCollection originCollection = source.getGranules(null);
            final DefaultProgressListener listener = new DefaultProgressListener();
            
            // Getting attributes structure to be filled
            final Collection<Property> destProps = feature.getProperties();
            final Set<Name> destAttributes = new HashSet<Name>();
            for (Property prop: destProps) {
                destAttributes.add(prop.getName());
            }
            
            // Collecting granules
            originCollection.accepts( new AbstractFeatureVisitor(){
                public void visit( Feature feature ) {
                    if(feature instanceof SimpleFeature)
                    {
                            // get the feature
                            final SimpleFeature sourceFeature = (SimpleFeature) feature;
                            final SimpleFeature destFeature = DataUtilities.template(indexSchema);
                            Collection<Property> props = sourceFeature.getProperties();
                            Name propName = null;
                            Object propValue = null;
                            
                            // Assigning value to dest feature for matching attributes 
                            for (Property prop: props) {
                                propName = prop.getName();
                                propValue = prop.getValue();
                                
                                if (destAttributes.contains(propName)) {
                                    destFeature.setAttribute(propName, propValue);
                                }
                            }
                            
                            //TODO DR: Need to put here the NetCDF Properties collector
                            
                            destFeature.setAttribute("runtime", fileBeingProcessed.lastModified());
                            destFeature.setAttribute(configuration.getLocationAttribute(), fileLocation);
                            updateAttributesFromCollectors(destFeature, fileBeingProcessed, inputReader, propertiesCollectors);
                            collection.add(destFeature);
                            
                            // check if something bad occurred
                            if(listener.isCanceled()||listener.hasExceptions()){
                                if(listener.hasExceptions())
                                    throw new RuntimeException(listener.getExceptions().peek());
                                else
                                    throw new IllegalStateException("Feature visitor has been canceled");
                            }
                    }
                }            
            }, listener);
            
        } else {
            
            //
            // Case B: old style reader, proceed with classic way, using properties collectors 
            // 
            feature.setAttribute(indexSchema.getGeometryDescriptor().getLocalName(),
                    GEOM_FACTORY.toGeometry(new ReferencedEnvelope((Envelope) envelope)));
            feature.setAttribute(configuration.getLocationAttribute(), fileLocation);
            
            updateAttributesFromCollectors(feature, fileBeingProcessed, inputReader, propertiesCollectors);
            collection.add(feature);
        }
        store.addGranules(collection);
    }

    /**
     * Update feature attributes through properties collector
     * @param feature
     * @param fileBeingProcessed
     * @param inputReader
     * @param propertiesCollectors
     */
    private static void updateAttributesFromCollectors(
            final SimpleFeature feature,
            final File fileBeingProcessed, 
            final AbstractGridCoverage2DReader inputReader,
            final List<PropertiesCollector> propertiesCollectors) {
        // collect and dump properties
        if (propertiesCollectors != null && propertiesCollectors.size() > 0)
            for (PropertiesCollector pc : propertiesCollectors) {
                pc.collect(fileBeingProcessed).collect(inputReader)
                        .setProperties(feature);
                pc.reset();
            }
        
    }

    /**
     * Prepare the location on top of the configuration and file to be processed.
     * @param runConfiguration
     * @param fileBeingProcessed
     * @return
     * @throws IOException
     */
    static private String prepareLocation(CatalogBuilderConfiguration runConfiguration, final File fileBeingProcessed) throws IOException {
        // absolute
        if (runConfiguration.isAbsolute()) {
            return fileBeingProcessed.getAbsolutePath();
        }

        // relative
        String path = fileBeingProcessed.getCanonicalPath();
        path = path.substring(runConfiguration.getRootMosaicDirectory().length());
        return path;

    }

}
