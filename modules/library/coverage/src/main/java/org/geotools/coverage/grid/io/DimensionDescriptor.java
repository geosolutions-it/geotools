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

/**
 * Dimension descriptor.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public interface DimensionDescriptor {
    
    /**
     * return the name of the dimension
     * @return
     */
    String getName();
    
    /**
     * return the type of the dimension
     * @return
     */
    Class<?> getType();
    
    /**
     * provide information of the dimension. return {@code true} in case of a range dimension,
     * false otherwise
     * @return
     */
    boolean isRange();
    
    /**
     * Return the main attribute's name in the feature related to this dimension
     * @return
     */
    String getAttributeName();
    
    /**
     * Return the second attribute name (if any) in the feature related to this dimension in
     * case of range dimension.
     * @return
     */
    String getEndAttributeName();
}
