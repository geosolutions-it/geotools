/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic.catalog.oracle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.transform.TransformFactory;
import org.geotools.feature.NameImpl;
import org.geotools.util.Utilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * A data store wrapper around a {@link DataStore} object.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @TODO move that class on gt-transform once ready
 */
public abstract class DataStoreWrapper implements DataStore {

    protected final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(DataStoreWrapper.class);

    /** Auxiliary folder which contains properties file with mapping information */ 
    protected File auxiliaryFolder;

    /** The underlying datastore */
    protected final DataStore datastore;

    /** Mapping between typeNames and FeatureTypeMapper */
    protected final Map<Name, FeatureTypeMapper> mapping = new ConcurrentHashMap<Name, FeatureTypeMapper>();

    /** Quick access typeNames list */
    private List<String> typeNames = new ArrayList<String>();

    /**
     * Base constructor 
     * @param datastore
     * @param auxFolderPath
     */
    public DataStoreWrapper(DataStore datastore, String auxFolderPath) {
        this.datastore = datastore;
        initMapping(auxFolderPath);
    }

    /**
     * Initialize the mapping by creating proper {@link FeatureTypeMapper}s on top of the available property files
     * which contain mapping information.
     * 
     * @param auxFolderPath the path of the folder containing mapping properties files
     */
    private void initMapping(String auxFolderPath) {
        URL url;
        try {
            url = new URL(auxFolderPath);
            File file = DataUtilities.urlToFile(url);
            if (!file.exists()) {
                // Pre-create folder when missing
                file.mkdir();
            } else if (file.isDirectory() || file.canRead()) {
                loadMappers(file);
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("The specified config folder for datastore wrapping can't be read or isn't a directory: " + auxFolderPath);
                }
            }
            this.auxiliaryFolder = file;
        } catch (MalformedURLException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("The specified config folder for datastore wrapping is not valid: " + auxFolderPath);
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("Unable to initialize the wrapping mapping for this folder: " + auxFolderPath);
            }
        }
    }

    /**
     * Load information from property files and initialize the related {@link FeatureTypeMapper}s
     * 
     * @param file
     * @throws Exception
     */
    private void loadMappers(final File file) throws Exception {
        // TODO we should do a lazy load initialization
        final String [] files = file.list();
        final String parentPath = file.getAbsolutePath();

        // Loop over files
        for (String element: files) {
            final Properties properties = loadProperties(parentPath + File.separatorChar + element);
            final FeatureTypeMapper mapper = getFeatureTypeMapper(properties);
            final Name name = mapper.getName();
            mapping.put(name, mapper);
            typeNames.add(name.getLocalPart());
            
        }
    }

    /**
     * Utility method which load properties from a propertiesFile.
     * @param propertiesFile
     * @return
     */
    private static Properties loadProperties(final String propertiesFile) {
        InputStream inStream = null;
        Properties properties = new Properties();
        try {
            File propertiesFileP = new File(propertiesFile);
            inStream = new BufferedInputStream(new FileInputStream(propertiesFileP));
            properties.load(inStream);
        } catch (FileNotFoundException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Unable to store the mapping " + e.getLocalizedMessage());
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Unable to store the mapping " + e.getLocalizedMessage());
            }

        } finally {
            if (inStream != null) {
                IOUtils.closeQuietly(inStream);
            }
        }            
        return properties;
    }

    @Override
    public ServiceInfo getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        if (featureType != null) {
            Name name = featureType.getName();
            if (!mapping.containsKey(name)) {
                // Initialize mapping
                try {
                    // Get a mapper for that featureType
                    final FeatureTypeMapper mapper = getFeatureTypeMapper(featureType);

                    // Store the mapper
                    storeMapper(mapper);

                    // Get the transformed featureType
                    final SimpleFeatureType mappedFeatureType = mapper.getMappedFeatureType();
                    datastore.createSchema(mappedFeatureType);
                    mapping.put(name, mapper);
                    typeNames.add(name.getLocalPart());
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        }
    }

    /**
     * Store the properties on disk
     * @param properties
     * @param typeName
     */
    protected void storeProperties(Properties properties, String typeName) {
        OutputStream outStream = null;
        try {
            final String propertiesPath = auxiliaryFolder.getAbsolutePath() + File.separatorChar + typeName + ".properties";
            outStream = new BufferedOutputStream(new FileOutputStream(new File(propertiesPath)));
            properties.store(outStream, null);
        } catch (FileNotFoundException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Unable to store the mapping " + e.getLocalizedMessage());
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Unable to store the mapping " + e.getLocalizedMessage());
            }

        } finally {
            if (outStream != null) {
                IOUtils.closeQuietly(outStream);
            }
        }
    }

    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Name> getNames() throws IOException {
        return new ArrayList<Name>(mapping.keySet());
    }

    @Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        if (mapping.containsKey(name)) {
            FeatureTypeMapper mapper = mapping.get(name);
            if (mapper != null) {
                return mapper.getWrappedFeatureType();
            }
        }
        return null;
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return getSchema(new NameImpl(typeName));
    }

    @Override
    public void dispose() {
        datastore.dispose();
    }

    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getTypeNames() throws IOException {
        return (String[]) typeNames.toArray(new String[typeNames.size()]);
    }

    @Override
    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        Utilities.ensureNonNull("typeName", typeName);
        return getFeatureSource(new NameImpl(typeName));
    }

    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        FeatureTypeMapper mapper = getMapper(typeName);
        if (mapper == null) {
            throw new IOException ("Undefined typeName");
        } else {
            SimpleFeatureStore source = (SimpleFeatureStore) datastore.getFeatureSource(mapper.getMappedName());
            return transformFeatureStore(source, mapper);
        }
    }
    
    protected SimpleFeatureSource transformFeatureStore(SimpleFeatureStore source,
            FeatureTypeMapper mapper) throws IOException {
        return TransformFactory.transform(source, mapper.getName(), mapper.getDefinitions());
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName,
            Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockingManager getLockingManager() {
        return datastore.getLockingManager();
    }

    /**
     * Return the mapper for the specified typeName
     * @param typeName
     * @return
     */
    private FeatureTypeMapper getMapper(Name typeName) {
        FeatureTypeMapper mapper = null;
        Utilities.ensureNonNull("typeName", typeName);
        if (mapping.containsKey(typeName)) {
           mapper = mapping.get(typeName);
        }
        return mapper;
    }

    /**
     * Return a specific {@link FeatureTypeMapper} instance on top of an input featureType
     * @param featureType
     * @return
     * @throws Exception
     */
    protected abstract FeatureTypeMapper getFeatureTypeMapper(final SimpleFeatureType featureType) throws Exception;

    /**
     * Return a specific {@link FeatureTypeMapper} by parsing mapping properties contained within
     * the specified {@link Properties} object
     * @param featureType
     * @return
     * @throws Exception
     */
    protected abstract FeatureTypeMapper getFeatureTypeMapper(final Properties properties) throws Exception;
    
    /**
     * Store the {@link FeatureTypeMapper} instance
     * @param mapper
     */
    protected abstract void storeMapper(FeatureTypeMapper mapper);

}
