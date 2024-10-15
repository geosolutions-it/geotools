/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2024, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.referencing.proj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A PROJ Dedicated Aliases Lookup that allows to retrieve PROJ Aliases for most common EPSG
 * Ellipsoids and PrimeMeridians
 */
public class PROJAliases {

    private static final String ALIAS_TABLE = "PROJAliases.txt";

    private Map<String, String> ellipsoidAliases = new HashMap<>();
    private Map<String, String> primeMeridianAliases = new HashMap<>();

    public PROJAliases() {
        URL aliasURL = PROJAliases.class.getResource(ALIAS_TABLE);

        try {
            try (BufferedReader br =
                    new BufferedReader(new InputStreamReader(aliasURL.openStream()))) {
                String line;
                Map<String, String> currentMap = null;

                while ((line = br.readLine()) != null) {
                    line = line.trim();

                    // Check for section headers
                    if (line.startsWith("[EllipsoidAliases]")) {
                        currentMap = ellipsoidAliases;
                        continue;
                    } else if (line.startsWith("[PrimeMeridianAliases]")) {
                        currentMap = primeMeridianAliases;
                        continue;
                    }
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(";");
                    if (parts.length == 2 && currentMap != null) {
                        String epsgVersion = parts[0].trim();
                        String projAlias = parts[1].trim();
                        currentMap.put(epsgVersion, projAlias);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load PROJ Aliases ", ioe.getCause());
        }
    }

    // Method to get the PROJ alias for a given EPSG ellipsoid name
    public String getEllipsoidAlias(String epsgEllipsoid) {
        return ellipsoidAliases.get(epsgEllipsoid);
    }

    // Method to get the PROJ alias for a given EPSG prime meridian name
    public String getPrimeMeridianAlias(String epsgPrimeMeridian) {
        return primeMeridianAliases.get(epsgPrimeMeridian);
    }
}
