/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.filter.function;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.filter.FilterFactoryImpl;
import org.junit.Test;
import org.opengis.filter.expression.Function;

public class FilterFunction_arrayOverlapTest {
    @Test
    public void testOverlappingIntegers() throws Exception {
        FilterFactoryImpl ff = new FilterFactoryImpl();
        Function func =
                ff.function(
                        "arrayOverlap",
                        ff.literal(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9}),
                        ff.literal(new Integer[] {1, 2, 3}));
        assertTrue((Boolean) func.evaluate(new Object()));
    }

    @Test
    public void testNotOverlappingIntegers() throws Exception {
        FilterFactoryImpl ff = new FilterFactoryImpl();
        Function func =
                ff.function(
                        "arrayOverlap",
                        ff.literal(new Integer[] {4, 5, 6, 7, 8, 9}),
                        ff.literal(new Integer[] {1, 2, 3}));
        assertFalse((Boolean) func.evaluate(new Object()));
    }

    @Test
    public void testOverlappingStrings() throws Exception {
        FilterFactoryImpl ff = new FilterFactoryImpl();
        Function func =
                ff.function(
                        "arrayOverlap",
                        ff.literal(new String[] {"a", "b", "c", "d"}),
                        ff.literal(new String[] {"a", "b", "e"}));
        assertTrue((Boolean) func.evaluate(new Object()));
    }

    @Test
    public void testNotOverlappingStrings() throws Exception {
        FilterFactoryImpl ff = new FilterFactoryImpl();
        Function func =
                ff.function(
                        "arrayOverlap",
                        ff.literal(new String[] {"a", "b", "c", "d"}),
                        ff.literal(new String[] {"e"}));
        assertFalse((Boolean) func.evaluate(new Object()));
    }
}
