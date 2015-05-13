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
package org.geotools.coverage.io.netcdf.crs;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A factory providing NetCDF/GRIB custom {@link CoordinateReferenceSystem} 
 * instances with the related custom EPSG.
 * 
 * @author Daniele Romagnoli - GeoSolutions
 *
 */
public class NetCDFCRSAuthorityFactory extends FactoryUsingWKT {
    public static final String SYSTEM_DEFAULT_USER_PROJ_FILE = "netcdf.projections";

    public NetCDFCRSAuthorityFactory() {
        super(null, MAXIMUM_PRIORITY);
    }

    public NetCDFCRSAuthorityFactory(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

    /**
     * Returns the URL to the property file that contains CRS definitions. 
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        String cust_proj_file = System.getProperty(SYSTEM_DEFAULT_USER_PROJ_FILE);

        // Attempt to load user-defined projections
        if( cust_proj_file != null ){
            File proj_file = new File(cust_proj_file);
    
            if (proj_file.exists()) {
                URL url = DataUtilities.fileToURL( proj_file );
                if( url != null ){
                    return url;
                }
                else {
                    LOGGER.log(Level.SEVERE, "Had troubles converting " + cust_proj_file + " to URL");
                }
            }
        }

        // Use the built-in property definitions
        cust_proj_file = "netcdf.projections.properties";

        return NetCDFCRSAuthorityFactory.class.getResource(cust_proj_file);
    }
}
