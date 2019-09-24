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
package org.geotools.data.postgis.filter;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.VolatileFunction;

/**
 * ANY function implementation for PostgreSQL <br>
 * Function name: pgAny <br>
 * example:
 *
 * <pre>     pgAny(array_of_integers)=5 </pre>
 *
 * @author Mauro Bartolomeoli, Geosolutions
 */
public class FilterFunction_pgAny extends FunctionExpressionImpl implements VolatileFunction {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "pgAny",
                    Boolean.class,
                    // required parameters:
                    FunctionNameImpl.parameter("array", Object.class));

    public FilterFunction_pgAny() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object feature) {
        throw new UnsupportedOperationException("Unsupported usage of ANY operator");
    }
}
