/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io.catalog;


/**
 * Simple interface for creating visitors to a {@link GranuleCatalog} implementation.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 * @source $URL$
 */
public interface GranuleCatalogVisitor{
    /**
     * Method that can be used to perform a visit to a {@link GranuleCatalog}.
     * 
     * @param granule the {@link CoverageSlice} we are visiting
     * @param o a sibling {@link Object} to pass along while visiting
     */
    public void visit(final CoverageSlice granule, Object o);
}
