/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
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
 *
 */
package org.geotools.coverage.io.netcdf;

import it.geosolutions.imageio.utilities.ImageIOUtilities;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import junit.framework.Assert;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.test.TestData;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

public class NetCDFReaderTest extends Assert {
    
    @Test
    @Ignore
    public void NetCDFTest() throws NoSuchAuthorityCodeException, FactoryException, IOException, ParseException {
        
        final URL testURL = TestData.url(this, "sample_polyphemus_output.nc");
        final Hints hints= new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:4326", true));
        // Get format
        final AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(testURL,hints);
        final NetCDFReader reader = (NetCDFReader) format.getReader(testURL, hints);
        
        assertNotNull(format);
        
        String[] names = reader.getGridCoverageNames();
        names = new String[]{names[1]};
        
//        NetCDFImageReader readerz = (NetCDFImageReader) new NetCDFImageReaderSpi().createReaderInstance();
//        readerz.setInput(testURL);
//        ImageIOUtilities.visualize(readerz.read(0), "orig", true);
//        System.in.read();
//        
        for (String coverageName : names) {
        
            final String[] metadataNames = reader.getMetadataNames(coverageName);
            assertNotNull(metadataNames);
            assertEquals(metadataNames.length,10);
            
            assertEquals("true", reader.getMetadataValue(coverageName, "HAS_TIME_DOMAIN"));
            final String timeMetadata = reader.getMetadataValue(coverageName, "TIME_DOMAIN");
            assertEquals("2012-04-01T00:00:00.000Z/2012-04-01T00:00:00.000Z,2012-04-01T01:00:00.000Z/2012-04-01T01:00:00.000Z,2012-04-01T02:00:00.000Z/2012-04-01T02:00:00.000Z,2012-04-01T03:00:00.000Z/2012-04-01T03:00:00.000Z,2012-04-01T04:00:00.000Z/2012-04-01T04:00:00.000Z,2012-04-01T05:00:00.000Z/2012-04-01T05:00:00.000Z,2012-04-01T06:00:00.000Z/2012-04-01T06:00:00.000Z,2012-04-01T07:00:00.000Z/2012-04-01T07:00:00.000Z,2012-04-01T08:00:00.000Z/2012-04-01T08:00:00.000Z,2012-04-01T09:00:00.000Z/2012-04-01T09:00:00.000Z,2012-04-01T10:00:00.000Z/2012-04-01T10:00:00.000Z,2012-04-01T11:00:00.000Z/2012-04-01T11:00:00.000Z,2012-04-01T12:00:00.000Z/2012-04-01T12:00:00.000Z,2012-04-01T13:00:00.000Z/2012-04-01T13:00:00.000Z,2012-04-01T14:00:00.000Z/2012-04-01T14:00:00.000Z,2012-04-01T15:00:00.000Z/2012-04-01T15:00:00.000Z,2012-04-01T16:00:00.000Z/2012-04-01T16:00:00.000Z,2012-04-01T17:00:00.000Z/2012-04-01T17:00:00.000Z,2012-04-01T18:00:00.000Z/2012-04-01T18:00:00.000Z,2012-04-01T19:00:00.000Z/2012-04-01T19:00:00.000Z,2012-04-01T20:00:00.000Z/2012-04-01T20:00:00.000Z,2012-04-01T21:00:00.000Z/2012-04-01T21:00:00.000Z,2012-04-01T22:00:00.000Z/2012-04-01T22:00:00.000Z,2012-04-01T23:00:00.000Z/2012-04-01T23:00:00.000Z", timeMetadata);
            assertNotNull(timeMetadata);
            assertEquals("2012-04-01T00:00:00.000Z", reader.getMetadataValue(coverageName, "TIME_DOMAIN_MINIMUM"));
            assertEquals("2012-04-01T23:00:00.000Z", reader.getMetadataValue(coverageName, "TIME_DOMAIN_MAXIMUM"));
            
            assertEquals("true", reader.getMetadataValue(coverageName, "HAS_ELEVATION_DOMAIN"));
            final String elevationMetadata = reader.getMetadataValue(coverageName, "ELEVATION_DOMAIN");
            assertNotNull(elevationMetadata);
            assertEquals(
                    "10.0/10.0,35.0/35.0,75.0/75.0,125.0/125.0,175.0/175.0,250.0/250.0,350.0/350.0,450.0/450.0,550.0/550.0,700.0/700.0,900.0/900.0,1250.0/1250.0,1750.0/1750.0,2500.0/2500.0",
                    elevationMetadata);
            assertEquals(14, elevationMetadata.split(",").length);
            assertEquals("10.0", reader.getMetadataValue(coverageName, "ELEVATION_DOMAIN_MINIMUM"));
            assertEquals("2500.0", reader.getMetadataValue(coverageName, "ELEVATION_DOMAIN_MAXIMUM"));

            // limit yourself to reading just a bit of it
            final ParameterValue<GridGeometry2D> gg =  AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            final GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope(coverageName);
            final GeneralEnvelope reducedEnvelope = new GeneralEnvelope(new double[] {
                    originalEnvelope.getLowerCorner().getOrdinate(0),
                    originalEnvelope.getLowerCorner().getOrdinate(1)},
                    new double[] { originalEnvelope.getMedian().getOrdinate(0),
                            originalEnvelope.getMedian().getOrdinate(1)});
            reducedEnvelope.setCoordinateReferenceSystem(reader.getCoordinateReferenceSystem(coverageName));

            MathTransform raster2model = reader.getOriginalGridToWorld(coverageName, PixelInCell.CELL_CENTER);
            final Dimension dim = new Dimension();
            GridEnvelope gridRange = reader.getOriginalGridRange(coverageName);
            dim.setSize(gridRange.getSpan(0) * 8.0, gridRange.getSpan(1) * 4.0);
            final Rectangle rasterArea = ((GridEnvelope2D) gridRange);
            rasterArea.setSize(dim);
            final GridEnvelope2D range = new GridEnvelope2D(rasterArea);
//            gg.setValue(new GridGeometry2D(range, originalEnvelope));
            gg.setValue(new GridGeometry2D(range, reducedEnvelope));
            
            final ParameterValue<List> time = ImageMosaicFormat.TIME.createValue();
            final SimpleDateFormat formatD = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            formatD.setTimeZone(TimeZone.getTimeZone("GMT"));
            final Date timeD = formatD.parse("2012-04-01T00:00:00.000Z");
            time.setValue(new ArrayList() {
                {
                    add(timeD);
                }
            });
        
            final ParameterValue<List> elevation = ImageMosaicFormat.ELEVATION.createValue();
            elevation.setValue(new ArrayList() {
                {
                    add(35d); // Elevation
                }
            });
            
            GeneralParameterValue[] values = new GeneralParameterValue[] {gg, time, elevation};
            GridCoverage2D coverage = reader.read(coverageName, values);
            ImageIOUtilities.visualize(coverage.getRenderedImage(), coverageName, true);
            System.in.read();
            reader.dispose();
        }
    }
    
    @Test
    @Ignore
    public void NetCDFTestBlackForest() throws NoSuchAuthorityCodeException, FactoryException, IOException, ParseException {
        
        final URL testURL = TestData.url(this, "O3_Blackforest_FC1_Surface_Hourly.nc");
        final Hints hints= new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:4326", true));
        // Get format
        final AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(testURL,hints);
        final NetCDFReader reader = (NetCDFReader) format.getReader(testURL, hints);
        
//        NetCDFImageReader readerz = (NetCDFImageReader) new NetCDFImageReaderSpi().createReaderInstance();
//        readerz.setInput(testURL);
//        ImageIOUtilities.visualize(readerz.read(0), "orig", true);
//        System.in.read();
//        
        
        assertNotNull(format);
        
        String[] names = reader.getGridCoverageNames();
        
        for (String coverageName : names) {
        
            final String[] metadataNames = reader.getMetadataNames(coverageName);
            assertNotNull(metadataNames);
            assertEquals(metadataNames.length,10);
            
            assertEquals("true", reader.getMetadataValue(coverageName, "HAS_TIME_DOMAIN"));
            final String timeMetadata = reader.getMetadataValue(coverageName, "TIME_DOMAIN");
            assertEquals("2012-05-08T00:00:00.000Z/2012-05-08T00:00:00.000Z,2012-05-08T01:00:00.000Z/2012-05-08T01:00:00.000Z,2012-05-08T02:00:00.000Z/2012-05-08T02:00:00.000Z,2012-05-08T03:00:00.000Z/2012-05-08T03:00:00.000Z,2012-05-08T04:00:00.000Z/2012-05-08T04:00:00.000Z,2012-05-08T05:00:00.000Z/2012-05-08T05:00:00.000Z,2012-05-08T06:00:00.000Z/2012-05-08T06:00:00.000Z,2012-05-08T07:00:00.000Z/2012-05-08T07:00:00.000Z,2012-05-08T08:00:00.000Z/2012-05-08T08:00:00.000Z,2012-05-08T09:00:00.000Z/2012-05-08T09:00:00.000Z,2012-05-08T10:00:00.000Z/2012-05-08T10:00:00.000Z,2012-05-08T11:00:00.000Z/2012-05-08T11:00:00.000Z,2012-05-08T12:00:00.000Z/2012-05-08T12:00:00.000Z,2012-05-08T13:00:00.000Z/2012-05-08T13:00:00.000Z,2012-05-08T14:00:00.000Z/2012-05-08T14:00:00.000Z,2012-05-08T15:00:00.000Z/2012-05-08T15:00:00.000Z,2012-05-08T16:00:00.000Z/2012-05-08T16:00:00.000Z,2012-05-08T17:00:00.000Z/2012-05-08T17:00:00.000Z,2012-05-08T18:00:00.000Z/2012-05-08T18:00:00.000Z,2012-05-08T19:00:00.000Z/2012-05-08T19:00:00.000Z,2012-05-08T20:00:00.000Z/2012-05-08T20:00:00.000Z,2012-05-08T21:00:00.000Z/2012-05-08T21:00:00.000Z,2012-05-08T22:00:00.000Z/2012-05-08T22:00:00.000Z,2012-05-08T23:00:00.000Z/2012-05-08T23:00:00.000Z", timeMetadata);
            assertNotNull(timeMetadata);
            assertEquals("2012-05-08T00:00:00.000Z", reader.getMetadataValue(coverageName, "TIME_DOMAIN_MINIMUM"));
            assertEquals("2012-05-08T23:00:00.000Z", reader.getMetadataValue(coverageName, "TIME_DOMAIN_MAXIMUM"));
            
            assertEquals("false", reader.getMetadataValue(coverageName, "HAS_ELEVATION_DOMAIN"));
            final String elevationMetadata = reader.getMetadataValue(coverageName, "ELEVATION_DOMAIN");
            assertNull(elevationMetadata);

            // limit yourself to reading just a bit of it
            final ParameterValue<GridGeometry2D> gg =  AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            final GeneralEnvelope envelope = reader.getOriginalEnvelope(coverageName);
            MathTransform raster2model = reader.getOriginalGridToWorld(coverageName, PixelInCell.CELL_CENTER);
            final Dimension dim = new Dimension();
            GridEnvelope gridRange = reader.getOriginalGridRange(coverageName);
//            dim.setSize(gridRange.getSpan(0) / 2.0, gridRange.getSpan(1) / 2.0);
//            final Rectangle rasterArea = ((GridEnvelope2D) gridRange);
//            rasterArea.setSize(dim);
//            final GridEnvelope2D range = new GridEnvelope2D(rasterArea);
//            gg.setValue(new GridGeometry2D(range, envelope));
            gg.setValue(new GridGeometry2D(gridRange, envelope));
            
            final ParameterValue<List> time = ImageMosaicFormat.TIME.createValue();
            final SimpleDateFormat formatD = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            formatD.setTimeZone(TimeZone.getTimeZone("GMT"));
            final Date timeD = formatD.parse("2012-05-08T12:00:00.000Z");
            time.setValue(new ArrayList() {
                {
                    add(timeD);
                }
            });
        
            GeneralParameterValue[] values = new GeneralParameterValue[] {time};
            GridCoverage2D coverage = reader.read(coverageName, values);
            ImageIOUtilities.visualize(coverage.getRenderedImage(), coverageName, true);
            System.in.read();
            reader.dispose();
        }
    }
}
