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
package org.geotools.renderer.lite.gridcoverage2d;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.util.AreaOpPropertyGenerator;

/**
 * An <code>OperationDescriptor</code> describing the "ShadedRelief" operation.
 * 
 * TODO: Move to JAI-EXT when ready
 */
class ShadedReliefDescriptor extends OperationDescriptorImpl {

    public static final double DEFAULT_AZIMUTH = 315;

    public static final double DEFAULT_ALTITUDE = 45;

    public static final double DEFAULT_Z = 100000;

    private static final double DEGREES_TO_METERS = 111120; 

    public static final double DEFAULT_SCALE = DEGREES_TO_METERS;

    
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The resource strings that provide the general documentation and specify the parameter list for a ShadedRelief operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "ShadedRelief" },
            { "LocalName", "ShadedRelief" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description", "desc" },
            { "Version", "ver" },
            { "arg0Desc", "Region of interest" },
            { "arg1Desc", "Input NoData" },
            { "arg2Desc", "Destination NoData" },
            { "arg3Desc", "X resolution" },
            { "arg4Desc", "Y resolution" },
            { "arg5Desc", "Vertical Exaggeration" },
            { "arg6Desc", "elevation unit to 2D unit scale ratio" },
            { "arg7Desc", "altitude" },
            { "arg8Desc", "azimuth" },
            { "arg9Desc", "algorithm" }};

    /** The parameter names for the ShadedRelief operation. */
    private static final String[] paramNames = { "roi", "nodata", "destNoData", "resX","resY", 
        "verticalExaggeration", "verticalScale", "altitude", "azimuth", "algorithm" };

    /** The parameter class types for the ShadedRelief operation. */
    private static final Class[] paramClasses = { javax.media.jai.ROI.class,
            it.geosolutions.jaiext.range.Range.class, Double.class, Double.class, Double.class, Double.class,
            Double.class, Double.class, Double.class, ShadedReliefAlgorithm.class};

    /** The parameter default values for the ShadedRelief operation. */
    private static final Object[] paramDefaults = { null, null, 0d, NO_PARAMETER_DEFAULT, 
            NO_PARAMETER_DEFAULT, 1d, 1d, DEFAULT_ALTITUDE, DEFAULT_AZIMUTH, ShadedReliefAlgorithm.ZEVENBERGEN_THORNE_COMBINED };

    /** Constructor. */
    public ShadedReliefDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "ShadedRelief" operation.
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }

    public static RenderedOp create(RenderedImage source0, ROI roi, Range nodata,
            double destNoData, double resX, double resY, double verticalExaggeration,
            double verticalScale, double altitude, double azimuth, ShadedReliefAlgorithm algorithm,
            RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("ShadedRelief", RenderedRegistryMode.MODE_NAME);

        // Setting sources
        pb.setSource("source0", source0);

        // Setting params
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destNoData);
        pb.setParameter("resX", resX);
        pb.setParameter("resY", resY);
        pb.setParameter("verticalExaggeration", verticalExaggeration);
        pb.setParameter("verticalScale", verticalScale);
        pb.setParameter("altitude", altitude);
        pb.setParameter("azimuth", azimuth);
        pb.setParameter("algorithm", algorithm);

        return JAI.create("ShadedRelief", pb, hints);
    }
}
