/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
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
import java.util.Set;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * Support for reading GridCoverage2D out of a persistent store with also
 * geospatial context information retrieval.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
public interface GridCoverage2DReader extends GridCoverageReader {
    
    /**
     * The time domain (comma separated list of values)
     */
    public static final String TIME_DOMAIN = "TIME_DOMAIN";
    
    /**
     * Time domain resolution (when using min/max/resolution)
     */
    public static final String TIME_DOMAIN_RESOLUTION = "TIME_DOMAIN_RESOLUTION";

    /**
     * If the time domain is available (or if a min/max/resolution approach has been chosen)
     */
    public static final String HAS_TIME_DOMAIN = "HAS_TIME_DOMAIN";

    /**
     * The time domain max value
     */
    public static final String TIME_DOMAIN_MAXIMUM = "TIME_DOMAIN_MAXIMUM";

    /**
     * The time domain min value
     */
    public static final String TIME_DOMAIN_MINIMUM = "TIME_DOMAIN_MINIMUM";

    /**
     * Whether the elevation is expressed as a full domain or min/max/resolution (true if domain
     * list available)
     */
    public static final String HAS_ELEVATION_DOMAIN = "HAS_ELEVATION_DOMAIN";

    /**
     * Elevation domain (comma separated list of values)
     */
    public static final String ELEVATION_DOMAIN = "ELEVATION_DOMAIN";

    /**
     * Elevation domain maximum value
     */
    public static final String ELEVATION_DOMAIN_MAXIMUM = "ELEVATION_DOMAIN_MAXIMUM";

    /**
     * Elevation domain minimum value
     */
    public static final String ELEVATION_DOMAIN_MINIMUM = "ELEVATION_DOMAIN_MINIMUM";

    /**
     * Elevation domain resolution
     */
    public static final String ELEVATION_DOMAIN_RESOLUTION = "ELEVATION_DOMAIN_RESOLUTION";

    /**
     * If a coverage has this property is means it been read straight out of a file without 
     * any sub-setting, it means the coverage represents the full contents of the file.
     * This can be used for different types of optimizations, such as avoiding reading
     * and re-encoding the original file when the original file would do.
     */
    public static final String FILE_SOURCE_PROPERTY = "OriginalFileSource";

    
    GeneralEnvelope getOriginalEnvelope();
    GeneralEnvelope getOriginalEnvelope(String coverageName);
    CoordinateReferenceSystem getCoordinateReferenceSystem();
    CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName);
    GridEnvelope getOriginalGridRange();
    GridEnvelope getOriginalGridRange(String coverageName);
    MathTransform getOriginalGridToWorld(PixelInCell pixInCell);
    MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell);
    GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException, IOException;
    GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters) throws IllegalArgumentException, IOException;
    Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException;
    Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName) throws IOException;
    double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution) throws IOException;
    double[] getReadingResolutions(String coverageName, OverviewPolicy policy, double[] requestedResolution) throws IOException;

}
