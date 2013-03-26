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
import java.util.List;

import org.geotools.util.Range;
import org.opengis.filter.Filter;

/**
* A dimension descriptor for "range" style dimensions, that is, dimensions having ranges of validity with a start and a end
*
* @author Andrea Aime - GeoSolutions SAS
*
* @param <T>
*/
public interface RangeDimensionDescriptor<T extends Comparable<? super T>> extends DimensionDescriptor<T> {

    /**
    * The name of the attribute holding the range start value in the GranuleSource feature type
    *
    * @return
    */
   public String getStartAttribute();

   /**
    * The name of the attribute holding the range end value in the GranuleSource feature type
    *
    * @return
    */
   public String getEndAttribute();

   /**
    * Returns the domain (either fully, or a page of it)
    *
    * @param filter Allows to specify a filter to get a subset of the domain. The only attributes
    *        names that can be used in the filter are the start and end attributes for this range
    *        dimension
    * @param offset The start of the page, must be zero or positive
    * @param limit The maximum number of items to be returned, or a negative value to pose no limit
    */
   List<Range<T>> getDomain(Filter filter, int offset, int limit) throws IOException;

}