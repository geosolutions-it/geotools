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
 * Default implementation of the {@link HarvestedFile} interface
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultHarvestedFile implements HarvestedFile {

    File file;

    boolean success;

    String message;

    public DefaultHarvestedFile(File file, boolean success, String message) {
        this.file = file;
        this.success = success;
        this.message = message;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
