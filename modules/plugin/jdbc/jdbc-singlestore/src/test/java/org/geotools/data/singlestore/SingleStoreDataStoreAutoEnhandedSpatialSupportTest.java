/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.jdbc.JDBCTestSupport;
import org.junit.Test;

/**
 * Tests that enhandedSpatialSupport flag is automatically and properly set based on identified MySQL version.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class SingleStoreDataStoreAutoEnhandedSpatialSupportTest extends JDBCTestSupport {
    @Override
    protected JDBCTestSetup createTestSetup() {
        return new SingleStoreTestSetup();
    }

    @Test
    public void testEnhancedSpatialSupportDetection() throws Exception {
        boolean isSingleStore56 = SingleStoreDataStoreFactory.isSingleStoreVersion56OrAbove(dataStore);
        boolean isSingleStore80 = SingleStoreDataStoreFactory.isSingleStoreVersion80OrAbove(dataStore);
        if (isSingleStore56) {
            assertTrue(((SingleStoreDialectBasic) dialect).getUsePreciseSpatialOps());
        } else if (isSingleStore80) {
            assertTrue(((SingleStoreDialectBasic) dialect).getUsePreciseSpatialOps());
        } else {
            assertFalse(((SingleStoreDialectBasic) dialect).getUsePreciseSpatialOps());
        }
    }
}
