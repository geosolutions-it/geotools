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
package org.geotools.imageio.unidata.cv;

import java.io.IOException;
import java.util.List;

import org.geotools.util.Utilities;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * @author User
 * @param <T>
 *
 */
public abstract class CoordinateVariable<T> {
    
    public static CoordinateVariable<?> create(CoordinateAxis1D coordinateAxis){
        Utilities.ensureNonNull("coordinateAxis", coordinateAxis);
        // If the axis is not numeric, we can't process any further. 
        if (!coordinateAxis.isNumeric()) {
            throw new IllegalArgumentException("Unable to process non numeric coordinate variable: "+coordinateAxis.toString());
        }
        
        // INITIALIZATION
        
        // AxisType?
        final AxisType axisType= coordinateAxis.getAxisType();
        switch(axisType){
        case GeoX:case GeoY: case GeoZ: case Height: case Lat: case Lon:
        case Pressure: case Spectral:
            return new NumericCoordinateVariable(coordinateAxis);
        case RunTime:case Time:
            return new TimeCoordinateVariable(coordinateAxis);
        default:
            throw new IllegalArgumentException("Unsupported axis type: "+axisType+ " for coordinate variable: "+coordinateAxis.toStringDebug());
        }
        
    }
  
    protected final Class<T> binding;
    
    protected final CoordinateAxis1D coordinateAxis;

    /**
     * @param binding
     * @param coordinateAxis
     */
    public CoordinateVariable(Class<T> binding, CoordinateAxis1D coordinateAxis) {
        Utilities.ensureNonNull("coordinateAxis", coordinateAxis);
        Utilities.ensureNonNull("binding", binding);
        this.binding = binding;
        this.coordinateAxis = coordinateAxis;  
    }

    public Class<T> getType() {
        return binding;
    }

    public String getUnit(){
        return coordinateAxis.getUnitsString();
    }

    public CoordinateAxis1D unwrap(){
        return coordinateAxis;
    }

    public AxisType getAxisType(){
        return coordinateAxis.getAxisType();
    }

    public String getName() {
        return coordinateAxis.getShortName();
    }

    public long getSize() throws IOException {
        return coordinateAxis.getSize();
    }
    
    abstract public T getMinimum() throws IOException;

    abstract public T getMaximum() throws IOException;
    
    abstract public T read(int index) throws IndexOutOfBoundsException;
    
    abstract public List<T> read() throws IndexOutOfBoundsException;
}
