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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geotools.filter.FilterFactoryImpl;
import org.junit.Test;
import org.opengis.filter.expression.Function;

public class FilterFunction_arrayFromStringTest {
    @Test
    public void testStringsToIntegers() throws Exception {
        FilterFactoryImpl ff = new FilterFactoryImpl();
        Function func =
                ff.function(
                        "arrayFromString",
                        ff.literal("1,2,3"),
                        ff.literal("java.lang.Integer"),
                        ff.literal(","));
        Object result = func.evaluate(new Object());
        assertTrue(result instanceof Integer[]);
        Integer[] values = (Integer[]) result;
        assertEquals(1, (int) values[0]);
        assertEquals(2, (int) values[1]);
        assertEquals(3, (int) values[2]);
    }

    /*@Test
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
    }*/
}
