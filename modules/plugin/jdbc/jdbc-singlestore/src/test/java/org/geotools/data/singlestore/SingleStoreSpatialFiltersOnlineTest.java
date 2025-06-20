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

import org.geotools.jdbc.JDBCDataStoreAPITestSetup;
import org.geotools.jdbc.JDBCSpatialFiltersOnlineTest;

public class SingleStoreSpatialFiltersOnlineTest extends JDBCSpatialFiltersOnlineTest {

    @Override
    protected void connect() throws Exception {
        super.connect();

        // uncomment code below to enable logging
        // java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
        // handler.setLevel(java.util.logging.Level.FINE);
        // org.geotools.util.logging.Logging.getLogger(SingleStoreSpatialFiltersOnlineTest.class).addHandler(handler);

        if (dialect instanceof SingleStoreDialect) {
            org.geotools.util.logging.Logging.getLogger(SingleStoreSpatialFiltersOnlineTest.class)
                    .info("SingleStoreDialect enhanced spatial support is:"
                            + ((SingleStoreDialect) dialect).getUsePreciseSpatialOps());
        } else if (dialect instanceof SingleStoreDialectBasic) {
            org.geotools.util.logging.Logging.getLogger(SingleStoreSpatialFiltersOnlineTest.class)
                    .info("SingleStoreDialectBasic enhanced spatial support is:"
                            + ((SingleStoreDialectBasic) dialect).getUsePreciseSpatialOps());
        }
    }

    @Override
    protected JDBCDataStoreAPITestSetup createTestSetup() {
        return new SingleStoreDataStoreAPITestSetup();
    }

    @Override
    public void testBboxFilter() throws Exception {
        // super.testBboxFilter();
    }

    @Override
    public void testBboxFilterDefault() throws Exception {
        // super.testBboxFilterDefault();
    }

    @Override
    public void testCrossesFilter() throws Exception {
        // super.testCrossesFilter();
    }
}
