/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.Arrays;
import java.util.List;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

public class FilterFunction_arrayOverlap extends FunctionExpressionImpl {
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "arrayOverlap",
                    parameter(
                            "arrayOverlap",
                            Boolean.class,
                            "Any",
                            "Returns true if the two arrays have at least one item in common"),
                    parameter("array1", Object[].class, "First Array", "first array of values"),
                    parameter("array2", Object[].class, "Second Array", "second array of values"));

    public FilterFunction_arrayOverlap() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        Object[] array1;
        Object[] array2;

        try { // attempt to get array and perform conversion
            array1 = (Object[]) getExpression(0).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #0 - expected type Object[]");
        }

        try { // attempt to get array and perform conversion
            array2 = (Object[]) getExpression(1).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #1 - expected type Object[]");
        }
        boolean overlaps = false;
        List<Object> list = Arrays.asList(array2);
        for (int count = 0; count < array1.length && !overlaps; count++) {
            if (list.contains(array1[count])) {
                overlaps = true;
            }
        }
        return Boolean.valueOf(overlaps);
    }
}
