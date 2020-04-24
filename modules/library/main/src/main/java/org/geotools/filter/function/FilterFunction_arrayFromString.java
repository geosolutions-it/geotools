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

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;
import org.opengis.filter.capability.FunctionName;

public class FilterFunction_arrayFromString extends FunctionExpressionImpl {
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "arrayFromString",
                    parameter(
                            "arrayFromString",
                            Object[].class,
                            "Any",
                            "Returns an arary of the requested type from a delimited string"),
                    parameter("string", String.class, "String", "delimited string"),
                    parameter(
                            "type", String.class, "Array type", "type the values will be cast to"),
                    parameter(
                            "delimiter",
                            String.class,
                            "Delimiter",
                            "string used to separate elements"));

    public FilterFunction_arrayFromString() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String input;
        String type;
        String delimiter;

        try { // attempt to get string and perform conversion
            input = (String) getExpression(0).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #0 - expected type String");
        }

        try { // attempt to get type and perform conversion
            type = (String) getExpression(1).evaluate(feature);
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #1 - expected type String");
        }

        try {
            delimiter = (String) getExpression(2).evaluate(feature);
        } catch (Exception e) { // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function between argument #2 - expected type String");
        }
        String[] values = input.split(delimiter);
        try {
            Class<?> typeClass = Class.forName(type);
            Object[] result =
                    (Object[]) java.lang.reflect.Array.newInstance(typeClass, values.length);

            for (int count = 0; count < values.length; count++) {
                result[count] = Converters.convert(values[count], typeClass);
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid type specified");
        }
    }
}
