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

import org.opengis.filter.Filter;

/**
 * Describes a "dimension" exposed by a structured grid coverage reader. The dimension can be either
 * a point or a range type, see PointDimensionDescriptor and RangeDomainDescriptor
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 * @param <T>
 */
public interface DimensionDescriptor<T> {

   /**
    * The dimension name
    *
    * @return
    */
   String getName();

   /**
    * The dimension data type
    *
    * @return
    */
   Class<T> getType();

   /**
    * The minimum value in the dimension domain
    */
   T getMinimum() throws IOException;

   /**
    * The maximum value in the dimension domain
    *
    * @return
    */
   T getMaximum() throws IOException;

   /**
    * The domain size
    *
    * @return
    */
   int getSize() throws IOException;

   /**
    * Returns the domain
    *
    * @param filter Allows to specify a filter to get a subset of the domain. The attribute names
    *        that can be used to build the filter are specified in the {@link DimensionDescriptor}
    *        sub-interfaces
    *
    * @param offset The start of the page, must be zero or positive
    * @param limit The maximum number of items to be returned, or a negative value to pose no limit
    * @return the returned list contains either point values (T) or range values (Range<T>)
    *         according to the domain nature
    */
   List<? extends Object> getDomain(Filter filter, int offset, int limit) throws IOException;

}

