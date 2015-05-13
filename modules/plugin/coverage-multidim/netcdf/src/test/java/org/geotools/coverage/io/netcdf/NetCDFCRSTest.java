/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io.netcdf;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.io.netcdf.crs.NetCDFProjection;
import org.geotools.coverage.io.netcdf.crs.NetCDFProjectionBuilder;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.DefiningConversion;
import org.geotools.referencing.operation.projection.AlbersEqualArea;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;

public class NetCDFCRSTest extends Assert {

    @Test
    public void testUTMDatasetSpatialRef() throws Exception {
        final File file = TestData.file(this, "utm.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            assertTrue(crs instanceof ProjectedCRS);
            ProjectedCRS projectedCRS = ((ProjectedCRS) crs);
            GeographicCRS baseCRS = projectedCRS.getBaseCRS();

            // Dealing with SPATIAL_REF Attribute
            assertTrue(CRS.equalsIgnoreMetadata(baseCRS, DefaultGeographicCRS.WGS84));
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            assertTrue(transform instanceof TransverseMercator);
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testAlbersEqualAreaDataset() throws Exception {
        final File file = TestData.file(this, "albersequal.nc");
        NetCDFReader reader = null;
        try {
            reader = new NetCDFReader(file, null);
            String[] coverages = reader.getGridCoverageNames();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem(coverages[0]);
            assertTrue(crs instanceof ProjectedCRS);
            ProjectedCRS projectedCRS = ((ProjectedCRS) crs);
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            assertTrue(transform instanceof AlbersEqualArea);
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }
    
    @Test
    public void testProjectionSetup() throws Exception {
       ParameterValueGroup params = NetCDFProjectionBuilder.buildProjectionParams(NetCDFProjection.LAMBERT_CONFORMAL_CONIC_1SP.getOGCName());
       params.parameter("central_meridian").setValue(-95.0);
       params.parameter("latitude_of_origin").setValue(25.0); 
       params.parameter("scale_factor").setValue(1.0); 
       params.parameter("false_easting").setValue(0.0); 
       params.parameter("false_northing").setValue(0.0); 

       Map<String, Number> ellipsoidParams = new HashMap<String, Number>();
       ellipsoidParams.put(NetCDFUtilities.SEMI_MAJOR, 6378137);
       ellipsoidParams.put(NetCDFUtilities.INVERSE_FLATTENING, 298.257223563);

       Ellipsoid ellipsoid = NetCDFProjectionBuilder.buildEllipsoid("WGS 84", ellipsoidParams);
       NetCDFProjectionBuilder.updateEllipsoidParams(params, ellipsoid);
       GeodeticDatum datum = NetCDFProjectionBuilder.buildGeodeticDatum("WGS_1984", ellipsoid);
       GeographicCRS geoCRS = NetCDFProjectionBuilder.buildGeographicCRS("WGS 84", datum);
       MathTransform transform = NetCDFProjectionBuilder.buildTransform(params);
       DefiningConversion conversionFromBase = NetCDFProjectionBuilder.buildConversionFromBase("lambert_conformal_mercator_1sp", transform);

       CoordinateReferenceSystem crs = NetCDFProjectionBuilder.buildProjectedCRS(Collections.singletonMap("name", "custom_lambert_conformal_conic_1sp"), geoCRS, conversionFromBase, transform);

       assertTrue(crs instanceof ProjectedCRS);
       ProjectedCRS projectedCRS = ((ProjectedCRS) crs);
       GeographicCRS baseCRS = projectedCRS.getBaseCRS();

       assertTrue(CRS.equalsIgnoreMetadata(baseCRS, DefaultGeographicCRS.WGS84));
       assertTrue(transform instanceof LambertConformal1SP);
    }
    
}
