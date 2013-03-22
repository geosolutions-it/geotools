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
package org.geotools.imageio.unidata;

import java.util.Set;

/**
 * Simple class to specify all the collecting rules which should be applied when collecting properties. How to deal with runtime attributes? Should we
 * convert time to ranges? Which variables are supported by the related mosaic and therefore which ones should be excluded by the index?
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class CollectingRules {

    public Set<String> getSupportedDictionary() {
        return supportedDictionary;
    }

    public void setSupportedDictionary(Set<String> supportedDictionary) {
        this.supportedDictionary = supportedDictionary;
    }

    public RuntimeManagement getRuntimeManagement() {
        return runtimeManagement;
    }

    public void setRuntimeManagement(RuntimeManagement runtimeManagement) {
        this.runtimeManagement = runtimeManagement;
    }

    enum RuntimeManagement {
        NONE, FROM_FILE_DATE, FROM_DATA, EXPLICIT_VALUE
    }

    enum TimeAttributeManagement {
        SIMPLE, CONVERT_TO_RANGE
    }

    /**
     * The dictionary containing variables which can be handled by the catalog. This should allow excluding files coming from different
     * models/different stores.
     */
    private Set<String> supportedDictionary;

    /** 
     * Should we include a runtime field into the index? This may be helpful to avoid overwrite when dealing with forecasts coming
     * from different runs of the same model which generate data with same time, elevation and file names.
     */
    private RuntimeManagement runtimeManagement;

}
