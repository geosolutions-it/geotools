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
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * ShadedRelief processing RenderedImageFactory
 * 
 * TODO: Move to JAI-EXT when ready
 * 
 */
class ShadedReliefRIF implements RenderedImageFactory {

    public ShadedReliefRIF() {
    }

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting the Layout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);

        // Getting source
        RenderedImage img = pb.getRenderedSource(0);

        // Getting parameters
        int paramIndex = 0;
        ROI roi = (ROI) pb.getObjectParameter(paramIndex++);
        Range nodata = (Range) pb.getObjectParameter(paramIndex++);
        double destinationNoData = pb.getDoubleParameter(paramIndex++);
        double resX = pb.getDoubleParameter(paramIndex++);
        double resY = pb.getDoubleParameter(paramIndex++);
        double verticalExaggeration = pb.getDoubleParameter(paramIndex++);
        double verticalScale = pb.getDoubleParameter(paramIndex++);
        double altitude = pb.getDoubleParameter(paramIndex++);
        double azimuth = pb.getDoubleParameter(paramIndex++);
        ShadedReliefAlgorithm algorithm = (ShadedReliefAlgorithm) pb
                .getObjectParameter(paramIndex++);

        return new ShadedReliefOpImage(img, hints, l, roi, nodata, destinationNoData, resX, resY,
                verticalExaggeration, verticalScale, altitude, azimuth, algorithm);
    }
}
