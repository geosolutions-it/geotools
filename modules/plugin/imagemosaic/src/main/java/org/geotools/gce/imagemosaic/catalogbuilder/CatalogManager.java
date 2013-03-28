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
package org.geotools.gce.imagemosaic.catalogbuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalogFactory;

public class CatalogManager {

    
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
            catalog = GranuleCatalogFactory.createGranuleCatalog(params, false, true, Utils.SHAPE_SPI);
        }

        return catalog;
    }

    public void createSchema(GranuleCatalog catalog) {

    }
}
