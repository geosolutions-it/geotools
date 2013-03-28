/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.grid.io;

import java.io.IOException;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A {@link GridCoverage2DReader} which exposes the underlying granule structure and allows to 
 * create and remove coverages.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public interface StructuredGridCoverage2DReader extends GridCoverage2DReader {

    /**
     * Returns the granule source for the specified coverage (might be null, if there is only one supported coverage)
     * 
     * @param coverageName the name of the specified coverage
     * @param readOnly a boolean indicating whether we may want modify the GranuleSource
     * @return the requested {@link GranuleSource}
     * @throws IOException
     * @throws UnsupportedOperationException
     */
    GranuleSource getGranules(String coverageName, boolean readOnly) throws IOException, UnsupportedOperationException;

//    /**
//     * Describes the dimensions supported by the specified coverage, if any. 
//     * (might be null, if there is only one supported coverage)
//     */
//    Set<DimensionDescriptor> getDimensionDescriptors(String coverageName);

    /**
     * Return whether this reader can modify the granule source 
     * @return
     */
    boolean isReadOnly();

    /**
     * Creates a granule store for a new coverage with the given feature type
     */
    void createCoverage(String coverageName, SimpleFeatureType schema/*, Set<DimensionDescriptor> dimensions*/) throws
                                               IOException, UnsupportedOperationException;

    /**
     * removes a granule store for the specified coverageName
     */
    boolean removeCoverage(String coverageName) throws IOException, UnsupportedOperationException;
}
