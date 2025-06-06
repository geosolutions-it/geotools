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

import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.jdbc.JDBCBooleanOnlineTest;
import org.geotools.jdbc.JDBCBooleanTestSetup;
import org.junit.Assert;
import org.junit.Test;

public class SingleStoreBooleanOnlineTest extends JDBCBooleanOnlineTest {

    @Override
    protected JDBCBooleanTestSetup createTestSetup() {
        return new SingleStoreBooleanTestSetup();
    }


    @Test
    public void testGetFeatures() throws Exception {
        try (FeatureReader r = this.dataStore.getFeatureReader(new Query(this.tname("b")), Transaction.AUTO_COMMIT)) {
            r.hasNext();
            SimpleFeature f = (SimpleFeature)r.next();
            Assert.assertEquals((byte)1, f.getAttribute("boolProperty"));
            r.hasNext();
            f = (SimpleFeature)r.next();
            Assert.assertEquals((byte)0, f.getAttribute("boolProperty"));
        }
    }

    @Test
    public void testGetSchema() throws Exception {
        SimpleFeatureType ft = this.dataStore.getSchema(this.tname("b"));
        Assert.assertEquals(Boolean.class, ft.getDescriptor("boolProperty").getType().getBinding());
    }
}
