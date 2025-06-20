/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.singlestore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import org.geotools.api.data.Parameter;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

/**
 * DataStoreFactory for SingleStore database.
 *
 * @author David Winslow, The Open Planning Project
 * @author Nikolaos Pringouris <nprigour@gmail.com> added support for MySQL versions 5.6 (and above)
 */
public class SingleStoreDataStoreFactory extends JDBCDataStoreFactory {
    /** parameter for database type */
    public static final Param DBTYPE = new Param(
            "dbtype", String.class, "Type", true, "singlestore", Collections.singletonMap(Parameter.LEVEL, "program"));
    /** Default port number for SingleStore */
    public static final Param PORT = new Param("port", Integer.class, "Port", true, 3306);
    /** Storage engine to use when creating tables */
    public static final Param STORAGE_ENGINE =
            new Param("storage engine", String.class, "Storage Engine", false, "MyISAM");

    /**
     * Enhanced Spatial Support is available from SingleStore version 5.6 and onward. This includes some differentiation
     * of the spatial function naming which generally follow the naming convention ST_xxxx. Moreover spatial operations
     * are performed with precise object shape and not with minimum bounding rectangles. As of version 8 it is the only
     * option.
     */
    public static final Param ENHANCED_SPATIAL_SUPPORT =
            new Param("enhancedSpatialSupport", Boolean.class, "Enhanced Spatial Support", false, false);

    protected boolean enhancedSpatialSupport = (boolean) ENHANCED_SPATIAL_SUPPORT.sample;

    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        // return new SingleStoreDialectPrepared(dataStore);
        return new SingleStoreDialectBasic(dataStore, enhancedSpatialSupport);
    }

    @Override
    public String getDisplayName() {
        return "SingleStore";
    }

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected String getDatabaseID() {
        return (String) DBTYPE.sample;
    }

    @Override
    public String getDescription() {
        return "SingleStore Database";
    }

    @Override
    protected String getValidationQuery() {
        return "select version()";
    }

    @Override
    protected void setupParameters(Map<String, Object> parameters) {
        super.setupParameters(parameters);
        parameters.put(DBTYPE.key, DBTYPE);
        parameters.put(PORT.key, PORT);
        parameters.put(STORAGE_ENGINE.key, STORAGE_ENGINE);

        parameters.remove(SCHEMA.key);
    }

    @Override
    protected JDBCDataStore createDataStoreInternal(JDBCDataStore dataStore, Map<String, ?> params) throws IOException {
        String storageEngine = (String) STORAGE_ENGINE.lookUp(params);
        if (storageEngine == null) {
            storageEngine = (String) STORAGE_ENGINE.sample;
        }

        Boolean enhancedSpatialFlag = (Boolean) ENHANCED_SPATIAL_SUPPORT.lookUp(params);
        if (enhancedSpatialFlag == null) {
            // enhanced spatial support should be enabled if SingleStore
            // version is at least 5.6.
            enhancedSpatialSupport = isSingleStoreVersion56OrAbove(dataStore);
        } else if (enhancedSpatialFlag && !isSingleStoreVersion56OrAbove(dataStore)) {
            dataStore.getLogger().info("SingleStore version does not support enhancedSpatialSupport. Disabling it");
            enhancedSpatialSupport = false;
        }

        SQLDialect dialect = dataStore.getSQLDialect();
        if (dialect instanceof SingleStoreDialectBasic) {
            ((SingleStoreDialectBasic) dialect).setStorageEngine(storageEngine);
            ((SingleStoreDialectBasic) dialect).setUsePreciseSpatialOps(enhancedSpatialSupport);
            ((SingleStoreDialectBasic) dialect)
                    .setSingleStoreVersion80OrAbove(this.isSingleStoreVersion80OrAbove(dataStore));
        } else {
            ((SingleStoreDialectPrepared) dialect).setStorageEngine(storageEngine);
            ((SingleStoreDialectPrepared) dialect).setUsePreciseSpatialOps(enhancedSpatialSupport);
            ((SingleStoreDialectPrepared) dialect)
                    .setSingleStoreVersion80OrAbove(this.isSingleStoreVersion80OrAbove(dataStore));
        }

        return dataStore;
    }

    /**
     * check if the version of SingleStore is greater than 5.6.
     *
     * @return {@code true} if the database is higher than 5.6
     */
    protected static boolean isSingleStoreVersion56OrAbove(JDBCDataStore dataStore) {
        boolean isSingleStoreVersion56OrAbove = false;
        try (Connection con = dataStore.getDataSource().getConnection()) {
            int major = con.getMetaData().getDatabaseMajorVersion();
            int minor = con.getMetaData().getDatabaseMinorVersion();
            isSingleStoreVersion56OrAbove = major > 5 || (major == 5 && minor > 6);
        } catch (SQLException | IllegalStateException e) {
            dataStore.getLogger().warning("Unable to determine database version. Message: " + e.getLocalizedMessage());
        }
        return isSingleStoreVersion56OrAbove;
    }
    /**
     * check if the version of SingleStore is 8.0 or greater. Needed to determine which syntax can be used for eg.
     * {@code ST_SRID()}
     *
     * @return {@code true} if the database varion is is 8.0 or greater
     */
    protected static boolean isSingleStoreVersion80OrAbove(JDBCDataStore dataStore) {
        boolean isSingleStoreVersion80OrAbove = false;
        try (Connection con = dataStore.getDataSource().getConnection()) {
            int major = con.getMetaData().getDatabaseMajorVersion();
            isSingleStoreVersion80OrAbove = (major >= 8);
        } catch (SQLException | IllegalStateException e) {
            dataStore.getLogger().warning("Unable to determine database version. Message: " + e.getLocalizedMessage());
        }
        return isSingleStoreVersion80OrAbove;
    }
}
