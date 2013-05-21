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
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import org.geotools.coverage.io.CoverageSourceDescriptor;
import org.geotools.coverage.io.catalog.CoverageSlice;
import org.geotools.coverage.io.catalog.CoverageSlicesCatalog;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Query;
import org.geotools.resources.coverage.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory2;

/**
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 * @source $URL$
 */
public abstract class GeoSpatialImageReader extends ImageReader {

    /** the coverage slices slicesCatalog currently stored as H2 DB */
    protected CoverageSlicesCatalog slicesCatalog = null;

    private final static FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;

    protected int numImages = -1;

    private String auxiliaryFilesPath = null;

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
        try {
            if (slicesCatalog != null) {
                slicesCatalog.dispose();
            }
        } catch (Throwable t) {
            
        } finally {
            slicesCatalog = null;
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

    
    protected void setCatalog(CoverageSlicesCatalog catalog) {
        slicesCatalog = catalog;
    }

    /**
     * Return the list of imageIndex related to the feature in the slicesCatalog
     * which result from the specified query.
     * 
     * @param filterQuery the filter query (temporal, vertical, name selection) to 
     * restrict the requested imageIndexes 
     * @return
     * @throws IOException
     */
    public List<Integer> getImageIndex(Query filterQuery) throws IOException {
//        Query query = new Query(/*slicesCatalog.getSchema().getTypeName()*/);
//        query.setFilter(FF.and(query.getFilter(), filterQuery.getFilter()));
        List<CoverageSlice> descs = slicesCatalog.getGranules(filterQuery);
        List<Integer> indexes = new ArrayList<Integer>();
        for (CoverageSlice desc : descs) {
            Integer index = (Integer) desc.getOriginator().getAttribute(CoverageSlice.Attributes.INDEX);
            indexes.add(index);
        }
        return indexes;
    }

    public String getAuxiliaryFilesPath() {
        return auxiliaryFilesPath;
    }

    public void setAuxiliaryFilesPath(String auxiliaryFilesPath) {
        this.auxiliaryFilesPath = auxiliaryFilesPath;
    }

    /**
     * Returns the underlying slicesCatalog. 
     * 
     * @return
     */
    public CoverageSlicesCatalog getCatalog() {
        return slicesCatalog;
    }
    
    /**
     * Init the slicesCatalog based on the provided parameters
     * 
     * @param parentLocation 
     * @param databaseName 
     * @throws IOException
     */
    protected void initCatalog(URL parentLocation, String databaseName) throws IOException {
        slicesCatalog = new CoverageSlicesCatalog(databaseName,parentLocation);
    }
}
