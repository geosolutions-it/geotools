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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * @author User
 *
 * TODO caching
 */
class NumericCoordinateVariable extends CoordinateVariable<Double>{
    
    private static class AxisValueGetter {
        
        CoordinateAxis1D axis1D;
        
        double[] values = null;
        
        public AxisValueGetter(CoordinateAxis axis) {
            if (axis.isNumeric() && axis instanceof CoordinateAxis1D) {
                axis1D = (CoordinateAxis1D) axis;
                values = axis1D.getCoordValues();
                Attribute scaleFactor = axis1D.findAttribute("scale_factor");
                Attribute offset = axis1D.findAttribute("offset");
                if (scaleFactor != null || offset != null) {
                    rescaleValues(scaleFactor, offset);
                }
    
            } else {
                throw new IllegalArgumentException(
                        "The specified axis doesn't represent a valid zeta Axis");
            }
        }
    
        private void rescaleValues(Attribute scaleFactor, Attribute offset) {
            DataType dataType = scaleFactor != null ? scaleFactor.getDataType() : offset.getDataType();
            if (dataType == DataType.DOUBLE) {
                double sf = scaleFactor != null ? scaleFactor.getNumericValue().doubleValue() : 1.0d; 
                double off = offset != null ? offset.getNumericValue().doubleValue() : 0.0d;
                for (int i = 0 ; i < values.length; i++) {
                    values[i] = ( sf * values[i]) + off;
                }    
            } else if (dataType == DataType.FLOAT) {
                float sf = scaleFactor != null ? scaleFactor.getNumericValue().floatValue() : 1.0f; 
                float  off = offset != null ? offset.getNumericValue().floatValue() : 0.0f;
                for (int i = 0 ; i < values.length; i++) {
                    values[i] = ( sf * values[i]) + off;
                }
            }
        }
    
        public double build(int index) {
            if (values != null && values.length > index) {
                return values[index];
            }
            return Double.NaN;
        }
        
        public int getNumValues() {
            return values.length;
        }
    }

    private double scaleFactor= Double.NaN;
    
    private double offsetFactor=Double.NaN;

    /**
     * @param binding
     * @param coordinateAxis
     */
    public NumericCoordinateVariable(CoordinateAxis1D coordinateAxis) {
        super(Double.class, coordinateAxis);      
        // If the axis is not numeric, we can't process any further. 
        if (!coordinateAxis.isNumeric()) {
            throw new IllegalArgumentException("Unable to process non numeric coordinate variable: "+coordinateAxis.toString());
        }
        
        // scale and offset
        Attribute scaleFactor = coordinateAxis.findAttribute("scale_factor");
        if(scaleFactor!=null){
            this.scaleFactor=scaleFactor.getNumericValue().doubleValue();
        }
        Attribute offsetFactor = coordinateAxis.findAttribute("offset");  
        if(offsetFactor!=null){
            this.offsetFactor=offsetFactor.getNumericValue().doubleValue();
        }
    }

    @Override
    public Double getMinimum() throws IOException {
        return coordinateAxis.getMinValue();
    }

    @Override
    public Double getMaximum() throws IOException {
        return coordinateAxis.getMaxValue();
    }

    @Override
    public Double read(int index) throws IndexOutOfBoundsException {
        if(index>=this.coordinateAxis.getSize()){
            throw new IndexOutOfBoundsException("index >= "+coordinateAxis.getSize());
        }
        double val = handleValues(index);
        return val;
    }

    /**
     * @param index
     * @return
     */
    private double handleValues(int index) {
        double val=coordinateAxis.getCoordValue(index);
        if(!Double.isNaN(scaleFactor)){
            val*=scaleFactor;
        }
        if(!Double.isNaN(offsetFactor)){
            val+=offsetFactor;
        }
        return val;
    }

    @Override
    public List<Double> read() throws IndexOutOfBoundsException {
        final List<Double> values= new ArrayList<Double>();
        final int num =coordinateAxis.getShape()[0];
        for (int i = 0; i < num; i++) {
            double val = handleValues(i);
            values.add(val);
        }
         return values;        
        
    }

}
