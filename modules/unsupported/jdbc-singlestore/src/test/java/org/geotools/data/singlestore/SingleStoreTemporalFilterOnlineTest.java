/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
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

import org.geotools.jdbc.JDBCDateTestSetup;
import org.geotools.jdbc.JDBCTemporalFilterOnlineTest;

public class SingleStoreTemporalFilterOnlineTest extends JDBCTemporalFilterOnlineTest {

    @Override
    protected JDBCDateTestSetup createTestSetup() {
        return new SingleStoreDateTestSetup();
    }

    // unclear what is going on, running
    // docker run     -d --name singlestoredb-dev     -e ROOT_PASSWORD="password"     -p 3306:3306 -p 9000:9000
    // ghcr.io/singlestore-labs/singlestoredb-dev:latest
    // works fine without the LocalDateTime conversion
    //    /**
    //     * Override to use a {@code java.time.LocalDateTime} instead of a {@code java.util.Date} because that is what
    // the
    //     * SingleStore JDBC driver always returns since 8.0.23 and there is no automatic conversion from
    // {@LocalDateTime} to
    //     * {@Date}.
    //     *
    //     * @param query actual query
    //     * @param dates expected dates
    //     * @throws Exception if any
    //     * @link https://osgeo-org.atlassian.net/browse/GEOT-6821
    //     */
    //    @Override
    //    protected void assertDatesMatch(Query query, String... dates) throws Exception {
    //        SimpleFeatureSource source = dataStore.getFeatureSource(tname("dates"));
    //
    //        assertEquals(dates.length, source.getCount(query));
    //
    //        SimpleFeatureCollection features = source.getFeatures(query);
    //        try (SimpleFeatureIterator it = features.features()) {
    //            int i = 0;
    //            while (it.hasNext()) {
    //                SimpleFeature f = it.next();
    //                LocalDateTime expected = new java.sql.Timestamp(date(dates[i++]).getTime()).toLocalDateTime();
    //
    //                assertEquals(expected, f.getAttribute(aname("dt")));
    //            }
    //        }
    //    }
}
