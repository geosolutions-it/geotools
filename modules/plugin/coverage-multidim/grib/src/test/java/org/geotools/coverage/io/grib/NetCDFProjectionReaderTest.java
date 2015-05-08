/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io.grib;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

public class NetCDFProjectionReaderTest extends Assert {

    @Before
    public void setUp() throws Exception {
        System.setProperty("NETCDF_FORCE_OPEN_CHECK", "true");
    }

    
    @Test
    public void testNetCDFCRS() throws IOException, FactoryException, TransformException {
//        File file = new File("C:\\data\\zamg\\inca\\TT_FC_INCA.grb2");
        File gribFile = TestData.file(this, "20140908_20.cip.grib2");
        NetCDFReader reader = new NetCDFReader(gribFile, null);
        String name = reader.getGridCoverageNames()[0];
        GeneralEnvelope envelope = reader.getOriginalEnvelope(name);
        System.out.println("Envelope = " + envelope);
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(name);
        
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        MathTransform transform = CRS.findMathTransform(wgs84, crs, true);
        Coordinate coord = new Coordinate(-126.138, 16.281);
        Coordinate coordDest = JTS.transform(coord, null, transform);
        System.out.println(coordDest); 
        
        
    }
}
