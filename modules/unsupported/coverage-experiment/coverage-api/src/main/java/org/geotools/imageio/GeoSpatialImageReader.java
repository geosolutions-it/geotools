/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.imageio;


import it.geosolutions.imageio.utilities.SoftValueHashMap;

import java.io.IOException;
import java.util.Collection;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import org.geotools.coverage.io.CoverageSourceDescriptor;
import org.opengis.feature.type.Name;

/**
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 * @source $URL$
 */
public abstract class GeoSpatialImageReader extends ImageReader {

    protected int numImages = -1;
    
    /** Internal Cache for CoverageSourceDescriptor.**/
    private final SoftValueHashMap<String, CoverageSourceDescriptor> coverageSourceDescriptorsCache= new SoftValueHashMap<String, CoverageSourceDescriptor>();

    protected GeoSpatialImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        checkImageIndex(imageIndex);
        if (ignoreMetadata) {
            return null;
        }
        throw new UnsupportedOperationException("ImageMetadata are not supported for this ImageReader");
    }
    
    @Override
    public void dispose() {
        super.dispose();
        synchronized (coverageSourceDescriptorsCache) {
            coverageSourceDescriptorsCache.clear();
        }
        
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        throw new UnsupportedOperationException("getStreamMetadata is not supported");
    }

    /**
     * Simple check of the specified image index. Valid indexes are belonging 
     * the range [0 - numRasters]. In case this constraint is not respected, an
     * {@link IndexOutOfBoundsException} is thrown.
     * 
     * @param imageIndex the index to be checked
     * 
     * @throw {@link IndexOutOfBoundsException} in case the provided imageIndex 
     * is not in the range of supported ones.
     */
    protected void checkImageIndex(final int imageIndex) {
        if (imageIndex < 0 || imageIndex >= numImages) {
            throw new IndexOutOfBoundsException("Invalid imageIndex. It should "
                    + (numImages > 0 ? ("belong the range [0," + (numImages - 1)) : "be 0"));
        }
    }

    public int getNumImages(final boolean allowSearch) throws IOException {
        return numImages;
    }


    /**
     * Return the name of coverages made available by this provider
     */
    public abstract Collection<Name> getNames();

    /**
     * The number of coverages made available by this provider.
     */
    public abstract int getCoveragesNumber();
    

    /**
     * Forces implementors to create the {@link CoverageSourceDescriptor} for the provided name.
     * 
     * @param name
     * @return
     */
    protected abstract CoverageSourceDescriptor createCoverageDescriptor(Name name);
    
    
    /**
     * 
     * @param name
     * @return
     */
    public CoverageSourceDescriptor getCoverageDescriptor(Name name){

        final String name_ = name.toString();
        synchronized (coverageSourceDescriptorsCache) {
            if(coverageSourceDescriptorsCache.containsKey(name_)){
                return coverageSourceDescriptorsCache.get(name_);
            }
            
            // create, cache and return
            CoverageSourceDescriptor cd = createCoverageDescriptor(name);
            coverageSourceDescriptorsCache.put(name_, cd);
            return cd;
        }
    }
}
