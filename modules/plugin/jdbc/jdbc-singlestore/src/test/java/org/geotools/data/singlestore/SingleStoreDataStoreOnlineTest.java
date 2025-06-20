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

import org.geotools.jdbc.JDBCDataStoreOnlineTest;
import org.geotools.jdbc.JDBCTestSetup;

/**
 * Data store test for SingleStore.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class SingleStoreDataStoreOnlineTest extends JDBCDataStoreOnlineTest {
    @Override
    protected JDBCTestSetup createTestSetup() {
        return new SingleStoreTestSetup();
    }

    @Override
    public void testCreateSchemaWithConstraints() throws Exception {
        // SingleStore does not complain if the string is too long, so we cannot run this test
    }

    @Override
    protected String getCLOBTypeName() {
        // CLOB is supported in SingleStore 8 but not in 5
        return "TEXT";
    }
}
