/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2025, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.geoparquet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.jdbc.JDBCDataStore;

/**
 * DataStoreFactory for GeoParquet files, powered by DuckDB.
 *
 * <p>This factory creates DataStore instances that can read and query GeoParquet format files, both local and remote.
 * GeoParquet is an open format for geospatial data that builds on the Apache Parquet columnar storage format, providing
 * efficient access to large geospatial datasets.
 *
 * <p>The implementation uses DuckDB and its extensions (spatial, parquet, httpfs) to handle the heavy lifting of
 * reading and querying Parquet files. This provides excellent performance and compatibility with various storage
 * backends including local files, HTTP/HTTPS, and S3.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * Map<String, Object> params = new HashMap<>();
 * params.put("dbtype", "geoparquet");
 * params.put("uri", "file:/path/to/data.parquet");
 *
 * DataStore store = DataStoreFinder.getDataStore(params);
 * }</pre>
 */
public class GeoParquetDataStoreFactory extends ForwardingDataStoreFactory<GeoParquetDataStoreFactoryDelegate>
        implements DataStoreFactorySpi {

    static final String GEOPARQUET = GeoParquetDataStoreFactoryDelegate.GEOPARQUET;

    public static final DataAccessFactory.Param DBTYPE = GeoParquetDataStoreFactoryDelegate.DBTYPE;
    public static final Param NAMESPACE = GeoParquetDataStoreFactoryDelegate.NAMESPACE;

    public static final DataAccessFactory.Param URI_PARAM = GeoParquetDataStoreFactoryDelegate.URI_PARAM;
    public static final DataAccessFactory.Param MAX_HIVE_DEPTH = GeoParquetDataStoreFactoryDelegate.MAX_HIVE_DEPTH;
    public static final Param FETCHSIZE = GeoParquetDataStoreFactoryDelegate.FETCHSIZE;
    public static final Param SCREENMAP = GeoParquetDataStoreFactoryDelegate.SCREENMAP;
    public static final Param SIMPLIFY = GeoParquetDataStoreFactoryDelegate.SIMPLIFY;

    public GeoParquetDataStoreFactory() {
        super(new GeoParquetDataStoreFactoryDelegate());
    }

    @Override
    public GeoparquetDataStore createDataStore(Map<String, ?> params) throws IOException {
        JDBCDataStore delegateStore = delegate.createDataStore(params);
        return new GeoparquetDataStore(delegateStore);
    }

    @Override
    public boolean canProcess(Map<String, ?> params) {
        Object uriObj = params.get(URI_PARAM.key);
        if (uriObj == null) {
            return false;
        }

        try {
            return isParquetCandidate(toURI(uriObj));
        } catch (Exception e) {
            return false;
        }
    }

    public static URI toURI(Object value) throws URISyntaxException, MalformedURLException {
        if (value instanceof URI) {
            return (URI) value;
        }
        if (value instanceof URL) {
            return ((URL) value).toURI();
        }
        if (value instanceof String) {
            // Try as URI first; fallback to URL->URI to allow things like "file:/path"
            try {
                return new URI((String) value);
            } catch (URISyntaxException ex) {
                return new URL((String) value).toURI();
            }
        }
        throw new IllegalArgumentException("Unsupported location type for URI/URL lookup: " + value.getClass());
    }

    private boolean isParquetCandidate(URI uri) {
        // 1. Check extension (fast path)
        String path = uri.getPath().toLowerCase(Locale.ROOT);
        if (!path.endsWith(".parquet") && !path.contains(".parquet?")) {
            return false;
        }

        URL url = null;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            // If we cannot check the file content but it's a .parquet file,
            // let's assume is a valid candidate
            return true;
        }
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            File f = new File(uri.getPath());
            if (!f.exists() || f.isDirectory()) {
                return false;
            }
        }

        // 3. Optional: check Parquet magic bytes
        try (InputStream is = url.openStream()) {
            byte[] magic = new byte[4];
            if (is.read(magic) != 4) return false;
            return magic[0] == 'P' && magic[1] == 'A' && magic[2] == 'R' && magic[3] == '1';
        } catch (IOException e) {
            return false;
        }
    }
}
