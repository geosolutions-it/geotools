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
 * Default implementation of the {@link DimensionDescriptor} interface
 * 
 * @author Daniele Romagnoli - GeoSolutions SAS
 */
public class DefaultDimensionDescriptor implements DimensionDescriptor {

    /**
     * UCUM Unit set
     */
    public static class UCUM {

        /**
         * An UCUM Unit instance simply made of name and symbol.
         */
        public static class UCUMUnit {

            private String name;

            private String symbol;

            public UCUMUnit(String name, String symbol) {
                super();
                this.name = name;
                this.symbol = symbol;
            }

            public String getName() {
                return name;
            }

            public String getSymbol() {
                return symbol;
            }
        }

        /** 
         * Commonly used UCUM units. In case this set will grow too much, we may consider
         * importing some UCUM specialized library.
         */
        public final static UCUMUnit TIME_UNITS = new UCUMUnit("second", "s");

        public final static UCUMUnit ELEVATION_UNITS = new UCUMUnit("meter", "m");

    }

    private String name;

    private String unitSymbol;

    private String units;

    private String startAttribute;

    private String endAttribute;

    public DefaultDimensionDescriptor(String name, String units, String unitSymbol, 
            String startAttribute, String endAttribute) {
        super();
        this.name = name;
        this.unitSymbol = unitSymbol;
        this.units = units;
        this.startAttribute = startAttribute;
        this.endAttribute = endAttribute;
    }

    public String getName() {
        return name;
    }

    public String getUnitSymbol() {
        return unitSymbol;
    }

    public String getUnits() {
        return units;
    }

    public String getStartAttribute() {
        return startAttribute;
    }

    public String getEndAttribute() {
        return endAttribute;
    }

}
