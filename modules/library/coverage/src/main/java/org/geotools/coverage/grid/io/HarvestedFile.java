/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
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

import java.io.File;

/**
 * Information about one of the files that have been processed by
 * {@link StructuredGridCoverage2DReader#harvest(String, File, org.geotools.factory.Hints)},
 * indicating whether the file was successfully ingested or not.
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public interface HarvestedFile {

    /**
     * The file that has been processed
     * 
     * @return
     */
    File getFile();

    /**
     * If true, the file has been ingested and generated new granules in the reader, false otherwise
     * 
     * @return
     */
    boolean success();

    /**
     * In case the file was not ingested, provides a reason why it was skipped
     * 
     * @return
     */
    String getMessage();
}
