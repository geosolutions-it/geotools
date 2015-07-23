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

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.IntegerSequence;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import org.geotools.image.ImageWorker;
import org.geotools.renderer.lite.gridcoverage2d.ShadedReliefAlgorithm.DataProcessor;
import org.geotools.renderer.lite.gridcoverage2d.ShadedReliefAlgorithm.DataProcessorInt;
import org.geotools.renderer.lite.gridcoverage2d.ShadedReliefAlgorithm.DataProcessorShort;
import org.geotools.renderer.lite.gridcoverage2d.ShadedReliefAlgorithm.ShadedReliefParameters;
import org.geotools.renderer.lite.gridcoverage2d.ShadedReliefAlgorithm.ProcessingCase;

import com.sun.media.jai.util.ImageUtil;

/**
 * ShadedRelief op Image.
 * TODO: Move to JAI-EXT when ready
 */
class ShadedReliefOpImage extends AreaOpImage {

   
    private static final BorderExtender EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_REFLECT);

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    /** Boolean indicating that NoData must be checked */
    protected final boolean hasNoData;

    /** NoData Range element */
    protected Range noData;

    /** Boolean indicating that no roi and no data check must be done */
    protected final boolean caseA;

    /** Boolean indicating that only roi check must be done */
    protected final boolean caseB;

    /** Boolean indicating that only no data check must be done */
    protected final boolean caseC;
    
    /** LookupTable used for checking if an input byte sample is a NoData */
    protected boolean[] lut;

    /** Boolean indicating that ROI must be checked */
    protected final boolean hasROI;

    /** ROI element */
    protected ROI roi;

    /** ROI bounds as a Shape */
    protected final Rectangle roiBounds;

    /** ROI related image */
    protected PlanarImage roiImage;

    /** Destination No Data value for Byte sources */
    protected byte destNoDataByte;

    /** Destination No Data value for Short sources */
    protected short destNoDataShort;

    /** Destination No Data value for Integer sources */
    protected int destNoDataInt;

    /** Destination No Data value for Float sources */
    protected float destNoDataFloat;

    /** Destination No Data value for Double sources */
    protected double destNoDataDouble;

    protected RenderedImage extendedIMG;

    protected Rectangle destBounds;

    private double noDataDouble;

    private int maxX;

    private int maxY;

    private ShadedReliefAlgorithm.ShadedReliefParameters params;

    private final static int FIXED_PADDING = 1;

    public ShadedReliefOpImage(RenderedImage source, RenderingHints hints, ImageLayout l,
            ROI roi,
            Range noData, double destinationNoData,
            double resX, double resY, double verticalExaggeration, double verticalScale,
            double altitude, double azimuth, ShadedReliefAlgorithm algorithm) {
        super(source, l, hints, true, EXTENDER, FIXED_PADDING, FIXED_PADDING, FIXED_PADDING, FIXED_PADDING);

        maxX = minX + width - 1;
        maxY = maxY + height - 1;

        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // Roi object
            this.roi = roi;
            roiBounds = roi.getBounds();
        } else {
            hasROI = false;
            this.roi = null;
            roiBounds = null;
        }

        // Getting datatype
        int dataType = source.getSampleModel().getDataType();

        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
            this.noDataDouble = noData.getMin().doubleValue();
        } else {
            hasNoData = false;
        }

        if (hasNoData && dataType == DataBuffer.TYPE_BYTE) {
            initBooleanNoDataTable();
        }
        
     // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        // Destination No Data value is clamped to the image data type
        this.destNoDataDouble = destinationNoData;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
            break;
        case DataBuffer.TYPE_USHORT:
            this.destNoDataShort = ImageUtil.clampRoundUShort(destinationNoData);
            break;
        case DataBuffer.TYPE_SHORT:
            this.destNoDataShort = ImageUtil.clampRoundShort(destinationNoData);
            break;
        case DataBuffer.TYPE_INT:
            this.destNoDataInt = ImageUtil.clampRoundInt(destinationNoData);
            break;
        case DataBuffer.TYPE_FLOAT:
            this.destNoDataFloat = ImageUtil.clampFloat(destinationNoData);
            break;
        case DataBuffer.TYPE_DOUBLE:
            break;
        default:
            throw new IllegalArgumentException("Wrong image data type");
        }

        this.params = new ShadedReliefParameters(resX, resY, verticalExaggeration, verticalScale, altitude,
                azimuth, algorithm);

        if (this.extender != null) {
            ImageWorker worker = new ImageWorker(source).setRenderingHints(hints)
                    .setNoData(this.noData)
                    .border(leftPadding, rightPadding, topPadding, bottomPadding, extender);
            extendedIMG = worker.getRenderedImage();
            this.destBounds = getBounds();
        } else {
            int x0 = getMinX() + leftPadding;
            int y0 = getMinY() + topPadding;

            int w = getWidth() - leftPadding - rightPadding;
            w = Math.max(w, 0);

            int h = getHeight() - topPadding - bottomPadding;
            h = Math.max(h, 0);

            this.destBounds = new Rectangle(x0, y0, w, h);
        }
    }

    private void initBooleanNoDataTable() {
        // Initialization of the boolean lookup table
        lut = new boolean[256];

        // Fill the lookuptable
        for (int i = 0; i < 256; i++) {
            boolean result = true;
            if (noData.contains((byte) i)) {
                result = false;
            }
            lut[i] = result;
        }
    }

    /**
     * Performs the computation on a specified rectangle. The sources are cobbled.
     * 
     * @param sources an array of source Rasters, guaranteed to provide all necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src = new RasterAccessor(source, srcRect, formatTags[0], getSourceImage(0)
                .getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        // ROI fields
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // ROI check
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = getImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        if (!hasROI || !roiDisjointTile) {
            switch (dst.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(src, dst, roiIter, roiContainsTile);
                break;
            default:
                throw new IllegalArgumentException("Wrong Data Type defined");
            }

            // If the RasterAccessor object set up a temporary buffer for the
            // op to write to, tell the RasterAccessor to write that data
            // to the raster no that we're done with it.
            if (dst.isDataCopy()) {
                dst.clampDataArrays();
                dst.copyDataToRaster();
            }
        } else {
            // Setting all as NoData
            double[] backgroundValues = new double[src.getNumBands()];
            Arrays.fill(backgroundValues, destNoDataDouble);
            ImageUtil.fillBackground(dest, destRect, backgroundValues);
        }

    }

    protected void byteLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException();
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException();
    }

    protected void floatLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException();
    }

    protected void doubleLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException();
    }

    public Raster computeTile(int tileX, int tileY) {
        if (!cobbleSources) {
            return super.computeTile(tileX, tileY);
        }
        // Special handling for Border Extender

        /* Create a new WritableRaster to represent this tile. */
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        /* Clip output rectangle to image bounds. */
        Rectangle rect = new Rectangle(org.x, org.y, sampleModel.getWidth(),
                sampleModel.getHeight());

        Rectangle destRect = rect.intersection(destBounds);
        if ((destRect.width <= 0) || (destRect.height <= 0)) {
            return dest;
        }

        /* account for padding in srcRectangle */
        PlanarImage s = getSourceImage(0);
        // Fix 4639755: Area operations throw exception for
        // destination extending beyond source bounds
        // The default dest image area is the same as the source
        // image area. However, when an ImageLayout hint is set,
        // this might be not true. So the destRect should be the
        // intersection of the provided rectangle, the destination
        // bounds and the source bounds.
        destRect = destRect.intersection(s.getBounds());
        Rectangle srcRect = new Rectangle(destRect);
        srcRect.x -= getLeftPadding();
        srcRect.width += getLeftPadding() + getRightPadding();
        srcRect.y -= getTopPadding();
        srcRect.height += getTopPadding() + getBottomPadding();

        /*
         * The tileWidth and tileHeight of the source image may differ from this tileWidth and tileHeight.
         */
        IntegerSequence srcXSplits = new IntegerSequence();
        IntegerSequence srcYSplits = new IntegerSequence();

        // there is only one source for an AreaOpImage
        s.getSplits(srcXSplits, srcYSplits, srcRect);

        // Initialize new sequences of X splits.
        IntegerSequence xSplits = new IntegerSequence(destRect.x, destRect.x + destRect.width);

        xSplits.insert(destRect.x);
        xSplits.insert(destRect.x + destRect.width);

        srcXSplits.startEnumeration();
        while (srcXSplits.hasMoreElements()) {
            int xsplit = srcXSplits.nextElement();
            int lsplit = xsplit - getLeftPadding();
            int rsplit = xsplit + getRightPadding();
            xSplits.insert(lsplit);
            xSplits.insert(rsplit);
        }

        // Initialize new sequences of Y splits.
        IntegerSequence ySplits = new IntegerSequence(destRect.y, destRect.y + destRect.height);

        ySplits.insert(destRect.y);
        ySplits.insert(destRect.y + destRect.height);

        srcYSplits.startEnumeration();
        while (srcYSplits.hasMoreElements()) {
            int ysplit = srcYSplits.nextElement();
            int tsplit = ysplit - getBottomPadding();
            int bsplit = ysplit + getTopPadding();
            ySplits.insert(tsplit);
            ySplits.insert(bsplit);
        }

        /*
         * Divide destRect into sub rectangles based on the source splits, and compute each sub rectangle separately.
         */
        int x1, x2, y1, y2;
        Raster[] sources = new Raster[1];

        ySplits.startEnumeration();
        for (y1 = ySplits.nextElement(); ySplits.hasMoreElements(); y1 = y2) {
            y2 = ySplits.nextElement();

            int h = y2 - y1;
            int py1 = y1 - getTopPadding();
            int py2 = y2 + getBottomPadding();
            int ph = py2 - py1;

            xSplits.startEnumeration();
            for (x1 = xSplits.nextElement(); xSplits.hasMoreElements(); x1 = x2) {
                x2 = xSplits.nextElement();

                int w = x2 - x1;
                int px1 = x1 - getLeftPadding();
                int px2 = x2 + getRightPadding();
                int pw = px2 - px1;

                // Fetch the padded src rectangle
                Rectangle srcSubRect = new Rectangle(px1, py1, pw, ph);
                sources[0] = extender != null ? extendedIMG.getData(srcSubRect) : s
                        .getData(srcSubRect);

                // Make a destRectangle
                Rectangle dstSubRect = new Rectangle(x1, y1, w, h);
                computeRect(sources, dest, dstSubRect);

                // Recycle the source tile
                if (s.overlapsMultipleTiles(srcSubRect)) {
                    recycleTile(sources[0]);
                }
            }
        }
        return dest;
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI. The method uses the Double-checked locking in order to maintain
     * thread-safety
     * 
     * @return
     */
    protected PlanarImage getImage() {
        PlanarImage img = roiImage;
        if (img == null) {
            synchronized (this) {
                img = roiImage;
                if (img == null) {
                    roiImage = img = roi.getAsImage();
                }
            }
        }
        return img;
    }

    

    protected void shortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset = srcScanlineOffset;
        int dstPixelOffset = dstScanlineOffset;
        double destValue = Double.NaN;
        DataProcessor data = new DataProcessorShort(srcData, hasNoData, noData, noDataDouble, params);
        
//        if (caseA || (caseB && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++) {
                    int sX = i + dstX;
                    int sY = j + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue = data.processWindow(window, i, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = ImageUtil.clampRoundShort(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
    }
    
    
    protected void intLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];

        int dstData[] = dstDataArrays[0];
        int srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset = srcScanlineOffset;
        int dstPixelOffset = dstScanlineOffset;
        double destValue = Double.NaN;
        DataProcessor data = new DataProcessorInt(srcData, hasNoData, noData, noDataDouble, params);
        
//        if (caseA || (caseB && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++) {
                    int sX = i + dstX;
                    int sY = j + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue = data.processWindow(window, i, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = ImageUtil.clampRoundInt(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
//            // ROI Check
//        } else if (caseB) {
//
//            for (int j = 0; j < dheight; j++) {
//                y0 = srcY + j;
//
//                for (int i = 0; i < dwidth; i++) {
//
//                    x0 = srcX + i;
//
//                    boolean inROI = false;
//                    // ROI Check
//                    for (int y = 0; y < 3 && !inROI; y++) {
//                        int yI = y0 + y;
//                        for (int x = 0; x < 3 && !inROI; x++) {
//                            int xI = x0 + x;
//                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
//                                inROI = true;
//                            }
//                        }
//                    }
//
//                    if (inROI) {
//                        int sX = i + dstX;
//                        int sY = j + dstY;
//                        ProcessingCase currentCase = getCase(sX, sY);
//                        destValue = data.processWindow(window, i, j, srcPixelOffset,
//                                centerScanlineOffset, currentCase);
//                        dstData[dstPixelOffset] = ImageUtil.clampRoundInt(algorithm.getValue(
//                                window, params));
//                    } else {
//                        dstData[dstPixelOffset] = destNoDataInt;
//                    }
//
//                    srcPixelOffset += srcPixelStride;
//                    dstPixelOffset += dstPixelStride;
//                }
//                srcScanlineOffset += srcScanlineStride;
//                dstScanlineOffset += dstScanlineStride;
//
//            }
//        // NoData Check
//    } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
//        for (int j = 0; j < dheight; j++) {
//            srcPixelOffset = srcScanlineOffset;
//            dstPixelOffset = dstScanlineOffset;
//            for (int i = 0; i < dwidth; i++) {
//                int sX = i + dstX;
//                int sY = j + dstY;
//                ProcessingCase currentCase = getCase(sX, sY);
//                destValue = data.processWindowNoData(window, i, j, srcPixelOffset, centerScanlineOffset, currentCase);
//                dstData[dstPixelOffset] = Double.isNaN(destValue) ? destNoDataInt : ImageUtil.clampRoundInt(destValue);
//                srcPixelOffset += srcPixelStride;
//                dstPixelOffset += dstPixelStride;
//            }
//            srcScanlineOffset += srcScanlineStride;
//            dstScanlineOffset += dstScanlineStride;
//        }
//        // ROI and No Data Check
//    } else {
//        for (int j = 0; j < dheight; j++) {
//            y0 = srcY + j;
//
//            for (int i = 0; i < dwidth; i++) {
//
//                x0 = srcX + i;
//
//                boolean inROI = false;
//                // ROI Check
//                for (int y = 0; y < 3 && !inROI; y++) {
//                    int yI = y0 + y;
//                    for (int x = 0; x < 3 && !inROI; x++) {
//                        int xI = x0 + x;
//                        if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
//                            inROI = true;
//                        }
//                    }
//                }
//
//                if (inROI) {
//                    int sX = i + dstX;
//                    int sY = j + dstY;
//                    ProcessingCase currentCase = getCase(sX, sY);
//                    destValue = data.processWindowNoData(window, i, j, srcPixelOffset,
//                            centerScanlineOffset, currentCase);
//                    dstData[dstPixelOffset] = Double.isNaN(destValue) ? destNoDataInt : ImageUtil.clampRoundInt(destValue);
//                } else {
//                    dstData[dstPixelOffset] = destNoDataInt;
//                }
//
//                srcPixelOffset += srcPixelStride;
//                dstPixelOffset += dstPixelStride;
//            }
//            srcScanlineOffset += srcScanlineStride;
//            dstScanlineOffset += dstScanlineStride;
//
//        }
//    }
    }
    
    private ProcessingCase getCase(int i, int j) {
        if (i == minX && j == minY) {
            return ProcessingCase.TOP_LEFT;
        } else if (i == maxX && j == minY) {
            return ProcessingCase.TOP_RIGHT;
        } else if (j == minY) {
            return ProcessingCase.TOP;
        } else if (i == minX && j == maxY) {
            return ProcessingCase.BOTTOM_LEFT;
        } else if (i == maxX && j == maxY) {
            return ProcessingCase.BOTTOM_RIGHT;
        } else if (i == minX) {
            return ProcessingCase.LEFT;
        } else if (i == maxX) {
            return ProcessingCase.RIGHT;
        } else if (j == maxY) {
            return ProcessingCase.BOTTOM;
        } else {
            return ProcessingCase.STANDARD;
        }

    }

}
