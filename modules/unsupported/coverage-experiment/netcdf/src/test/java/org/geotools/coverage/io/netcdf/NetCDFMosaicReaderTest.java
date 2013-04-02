/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
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

import it.geosolutions.imageio.utilities.ImageIOUtilities;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.swing.JFrame;

import junit.framework.JUnit4TestAdapter;
import junit.textui.TestRunner;

import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.test.TestData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

/**
 * Testing {@link ImageMosaicReader}.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Stefan Alfons Krueger (alfonx), Wikisquare.de
 * @since 2.3
 * 
 * 
 * 
 * @source $URL$
 */
public class NetCDFMosaicReaderTest extends Assert {

    private final static Logger LOGGER = Logger.getLogger(NetCDFMosaicReaderTest.class.toString());

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(NetCDFMosaicReaderTest.class);
    }

    private static boolean INTERACTIVE;

    /**
     * Simple test method accessing time and 2 custom dimensions for the sample dataset
     * 
     * @throws IOException
     * @throws FactoryException
     * @throws NoSuchAuthorityCodeException
     * @throws ParseException +
     */
    @Test
    @Ignore
    @SuppressWarnings("rawtypes")
    public void timeAdditionalDimRanges() throws Exception {
        final String dlrFolder = "C:\\data\\dlr\\samplesForMosaic\\";
        final File file = new File(dlrFolder);
        final URL url = DataUtilities.fileToURL(file);
        final Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode(
                "EPSG:4326", true));
        
        // Get format
        final AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(url, hints);
        final ImageMosaicReader reader = getReader(url, format);
        String[] names = reader.getGridCoverageNames();
        
        for (String name: names) {
            LOGGER.info("Coverage: " + name);
            final String[] metadataNames = reader.getMetadataNames(name);
            assertNotNull(metadataNames);
            assertEquals(metadataNames.length, 12);
            assertEquals("true", reader.getMetadataValue(name, "HAS_TIME_DOMAIN"));
            assertEquals(
                    "2012-04-01T00:00:00.000Z,2012-04-01T01:00:00.000Z,2012-04-01T02:00:00.000Z,2012-04-01T03:00:00.000Z,2012-04-01T04:00:00.000Z,2012-04-01T05:00:00.000Z,2012-04-01T06:00:00.000Z,2012-04-01T07:00:00.000Z,2012-04-01T08:00:00.000Z,2012-04-01T09:00:00.000Z,2012-04-01T10:00:00.000Z,2012-04-01T11:00:00.000Z,2012-04-01T12:00:00.000Z,2012-04-01T13:00:00.000Z,2012-04-01T14:00:00.000Z,2012-04-01T15:00:00.000Z,2012-04-01T16:00:00.000Z,2012-04-01T17:00:00.000Z,2012-04-01T18:00:00.000Z,2012-04-01T19:00:00.000Z,2012-04-01T20:00:00.000Z,2012-04-01T21:00:00.000Z,2012-04-01T22:00:00.000Z,2012-04-01T23:00:00.000Z",
                    reader.getMetadataValue(name, "TIME_DOMAIN"));
            assertEquals("2012-04-01T00:00:00.000Z", reader.getMetadataValue(name, "TIME_DOMAIN_MINIMUM"));
            assertEquals("2012-04-01T23:00:00.000Z", reader.getMetadataValue(name, "TIME_DOMAIN_MAXIMUM"));
        
            assertEquals("true", reader.getMetadataValue(name, "HAS_ELEVATION_DOMAIN"));
            assertEquals("10.0,35.0,75.0,125.0,175.0,250.0,350.0,450.0,550.0,700.0,900.0,1250.0,1750.0,2500.0", reader.getMetadataValue(name, "ELEVATION_DOMAIN"));
            assertEquals("10.0", reader.getMetadataValue(name, "ELEVATION_DOMAIN_MINIMUM"));
            assertEquals("2500.0", reader.getMetadataValue(name, "ELEVATION_DOMAIN_MAXIMUM"));
            
            assertEquals("true", reader.getMetadataValue(name, "HAS_RUNTIME_DOMAIN"));
            assertEquals("2012-05-09T12:29:30.000Z,2013-03-30T16:15:58.648Z", reader.getMetadataValue(name, "RUNTIME_DOMAIN"));
            assertEquals("2012-05-09T12:29:30.000Z", reader.getMetadataValue(name, "RUNTIME_DOMAIN_MINIMUM"));
            assertEquals("2013-03-30T16:15:58.648Z", reader.getMetadataValue(name, "RUNTIME_DOMAIN_MAXIMUM"));
        
            // use imageio with defined tiles
            final ParameterValue<Boolean> useJai = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
            useJai.setValue(false);
        
            // specify time
            final ParameterValue<List> time = ImageMosaicFormat.TIME.createValue();
            final Date timeD = parseTimeStamp("2012-04-01T00:00:00.000Z");
            time.setValue(new ArrayList() {
                {
                    add(timeD);
                }
            });
        
            final ParameterValue<List> elevation = ImageMosaicFormat.ELEVATION.createValue();
            elevation.setValue(new ArrayList() {
                {
                    add(75d); // Elevation
                }
            });
        
            Set<ParameterDescriptor<List>> params = reader.getDynamicParameters(name);
            ParameterValue<List<String>> runtime = null;
            final String selectedWaveLength = "2013-03-30T16:15:58.648Z";
            for (ParameterDescriptor param : params) {
                if (param.getName().getCode().equalsIgnoreCase("RUNTIME")) {
                    runtime = param.createValue();
                    runtime.setValue(new ArrayList<String>() {
                        {
                            add(selectedWaveLength);
                        }
                    });
                }
            }
            assertNotNull(runtime);
            
            // Test the output coverage
            GeneralParameterValue[] values = new GeneralParameterValue[] { useJai, time, elevation, runtime};
            final GridCoverage2D coverage = (GridCoverage2D) reader.read(name, values);
            Assert.assertNotNull(coverage);
            final String fileSource = (String) coverage
                    .getProperty(AbstractGridCoverage2DReader.FILE_SOURCE_PROPERTY);
        
            // Check the proper granule has been read
            final String baseName = FilenameUtils.getBaseName(fileSource);
            assertEquals(baseName, "20130102polyphemus");
        }
    }

    private Date parseTimeStamp(String timeStamp) throws ParseException {
        final SimpleDateFormat formatD = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatD.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatD.parse(timeStamp);
    }

    /**
     * Shows the provided {@link RenderedImage} ina {@link JFrame} using the provided <code>title</code> as the frame's title.
     * 
     * @param image to show.
     * @param title to use.
     */
    static void show(RenderedImage image, String title) {
        ImageIOUtilities.visualize(image, title);

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        TestRunner.run(NetCDFMosaicReaderTest.suite());

    }

    @Before
    public void init() {

        // make sure CRS ordering is correct
        System.setProperty("org.geotools.referencing.forceXY", "true");
        System.setProperty("user.timezone", "GMT");
        System.setProperty("org.geotools.shapefile.datetime", "true");

        INTERACTIVE = TestData.isInteractiveTest();
    }

    @Before
    public void setUp() throws Exception {
        // remove generated file

        cleanUp();

    }

    /**
     * Cleaning up the generated files (shape and properties so that we recreate them.
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void cleanUp() throws FileNotFoundException, IOException {
        if (INTERACTIVE)
            return;
    }

    @After
    public void tearDown() throws FileNotFoundException, IOException {
        cleanUp();

    }

    @AfterClass
    public static void close() {
        System.clearProperty("org.geotools.referencing.forceXY");
    }

    /**
     * returns an {@link AbstractGridCoverage2DReader} for the provided {@link URL} and for the providede {@link AbstractGridFormat}.
     * 
     * @param testURL points to a valid object to create an {@link AbstractGridCoverage2DReader} for.
     * @param format to use for instantiating such a reader.
     * @return a suitable {@link ImageMosaicReader}.
     * @throws FactoryException
     * @throws NoSuchAuthorityCodeException
     */
    static ImageMosaicReader getReader(URL testURL, final AbstractGridFormat format)
            throws NoSuchAuthorityCodeException, FactoryException {

        // final Hints hints= new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:4326", true));
        return getReader(testURL, format, null);

    }

    static ImageMosaicReader getReader(URL testURL, final AbstractGridFormat format, Hints hints) {
        // Get a reader
        final ImageMosaicReader reader = (ImageMosaicReader) format.getReader(testURL, hints);
        Assert.assertNotNull(reader);
        return reader;
    }

}
