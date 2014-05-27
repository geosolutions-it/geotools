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
package org.geotools.data.sqlserver;

import java.io.IOException;

import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.Contains;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Same as {@link SQLServerSpatialFiltersTest}, but forcing the sql hints for spatial filters
 * 
 * @source $URL$
 */
public class SQLServerTableHintsTest extends SQLServerSpatialFiltersTest {

    @Override
    protected void connect() throws Exception {
        super.connect();

        SQLServerDialect dialect = (SQLServerDialect) dataStore.getSQLDialect();
        dialect.setForceSpatialIndexes(true);
        dialect.setTableHints(null);
    }
    
    public void testDecorateWithIndex() throws IOException {
        SQLServerDialect dialect = (SQLServerDialect) dataStore.getSQLDialect();
        StringBuffer sql = decorateSpatialQuery(dialect);

        assertTrue(sql.toString().contains("FROM \"road\" WITH(INDEX(\"_road_geometry_index\"))"));
    }

    public void testDecorateWithIndexAndTableHints() throws IOException {
        SQLServerDialect dialect = (SQLServerDialect) dataStore.getSQLDialect();
        dialect.setTableHints("NOLOCK");
        StringBuffer sql = decorateSpatialQuery(dialect);

        assertTrue(sql.toString().contains(
                "FROM \"road\" WITH(INDEX(\"_road_geometry_index\"), NOLOCK)"));
    }

    private StringBuffer decorateSpatialQuery(SQLServerDialect dialect) throws IOException {
        StringBuffer sql = new StringBuffer("SELECT \"fid\",\"id\",\"geom\".STAsBinary() as \"geom\",\"name\" "
                + "FROM \"road\" "
                + "WHERE  \"geom\".Filter(geometry::STGeomFromText('POLYGON ((2 -1, 2 5, 4 5, 4 -1, 2 -1))', 4326)) = 1 "
                + "AND geometry::STGeomFromText('POLYGON ((2 -1, 2 5, 4 5, 4 -1, 2 -1))', 4326).STContains(\"geom\") = 1");
        
        // the filter for the Query
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        GeometryFactory gf = new GeometryFactory();
        PackedCoordinateSequenceFactory sf = new PackedCoordinateSequenceFactory();
        LinearRing shell = gf.createLinearRing(sf.create(new double[] { 2, -1, 2, 5, 4, 5, 4, -1,
                2, -1 }, 2));
        Polygon polygon = gf.createPolygon(shell, null);
        Contains cs = ff.contains(ff.literal(polygon), ff.property(aname("geom")));

        
        SimpleFeatureType roadSchema = dataStore.getSchema("road");
        dialect.handleSelectHints(sql, roadSchema, new Query("road", cs));
        return sql;
    }

    public void testNonSpatialNoTableHints() throws IOException {
        SQLServerDialect dialect = (SQLServerDialect) dataStore.getSQLDialect();
        StringBuffer sql = new StringBuffer(
                "SELECT \"fid\",\"id\",\"geom\".STAsBinary() as \"geom\",\"name\" "
                        + "FROM \"road\" "
                        + "WHERE \"name\" = 'XXX')");

        // the filter for the Query
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Filter filter = ff.equal(ff.property("name"), ff.literal("XXX"), true);

        SimpleFeatureType roadSchema = dataStore.getSchema("road");
        dialect.handleSelectHints(sql, roadSchema, new Query("road", filter));

        assertFalse(sql.toString().contains("WITH"));
    }

    public void testNonSpatialWithTableHints() throws IOException {
        SQLServerDialect dialect = (SQLServerDialect) dataStore.getSQLDialect();
        dialect.setTableHints("NOLOCK");
        StringBuffer sql = new StringBuffer(
                "SELECT \"fid\",\"id\",\"geom\".STAsBinary() as \"geom\",\"name\" "
                        + "FROM \"road\" " 
                        + "WHERE \"name\" = 'XXX')");

        // the filter for the Query
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Filter filter = ff.equal(ff.property("name"), ff.literal("XXX"), true);

        SimpleFeatureType roadSchema = dataStore.getSchema("road");
        dialect.handleSelectHints(sql, roadSchema, new Query("road", filter));

        assertTrue(sql.toString().contains("WITH(NOLOCK)"));
    }

}
