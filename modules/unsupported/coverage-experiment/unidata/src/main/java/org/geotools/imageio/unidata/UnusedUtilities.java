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
package org.geotools.imageio.unidata;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * @author User
 *
 */
@Deprecated // methods in this class might die soon
abstract public class UnusedUtilities {

    private static class KeyValuePair implements Map.Entry<String, String> {
    
        public KeyValuePair(final String key, final String value) {
            this.key = key;
            this.value = value;
        }
    
        private String key;
    
        private String value;
    
        public String getKey() {
            return key;
        }
    
        public String getValue() {
            return value;
        }
    
        private boolean equal(Object a, Object b) {
            return a == b || a != null && a.equals(b);
        }
    
        public boolean equals(Object o) {
            return o instanceof KeyValuePair && equal(((KeyValuePair) o).key, key)
                    && equal(((KeyValuePair) o).value, value);
        }
    
        private static int hashCode(Object a) {
            return a == null ? 42 : a.hashCode();
        }
    
        public int hashCode() {
            return hashCode(key) * 3 + hashCode(value);
        }
    
        public String toString() {
            return "(" + key + "," + value + ")";
        }
    
        public String setValue(String value) {
            this.value = value;
            return value;
        }
    }

    private static KeyValuePair getGlobalAttribute(final NetcdfDataset dataset, final int attributeIndex) throws IOException {
    	KeyValuePair attributePair = null;
        if (dataset != null) {
        	final List<Attribute> globalAttributes = dataset.getGlobalAttributes();
            if (globalAttributes != null && !globalAttributes.isEmpty()) {
            	final Attribute attribute = (Attribute) globalAttributes.get(attributeIndex);
                if (attribute != null) {
                    attributePair = new KeyValuePair(attribute.getFullName(), getAttributesAsString(attribute));
                }
            }
        }
        return attributePair;
    }

    private static KeyValuePair getAttribute(final Variable var, final int attributeIndex) {
    	KeyValuePair attributePair = null;
    	if (var != null){
    		final List<Attribute> attributes = var.getAttributes();
    	    if (attributes != null && !attributes.isEmpty()) {
    	    	final Attribute attribute = (Attribute) attributes.get(attributeIndex);
    	        if (attribute != null) {
    	            attributePair = new KeyValuePair(attribute.getFullName(),getAttributesAsString(attribute));
    	        }
    	    }
    	}
        return attributePair;
    }

    /**
         * Return a global attribute as a {@code String}. The required global
         * attribute is specified by name
         * 
         * @param attributeName
         *                the name of the required attribute.
         * @return the value of the required attribute. Returns an empty String in
         *         case the required attribute is not found.
         */
    private static String getGlobalAttributeAsString(final NetcdfDataset dataset, final String attributeName) {
            String attributeValue = "";
            if (dataset != null) {
            	final Attribute attrib = dataset.findGlobalAttribute(attributeName);
    //        	final List<Attribute> globalAttributes = dataset.getGlobalAttributes();
    //            if (globalAttributes != null && !globalAttributes.isEmpty()) {
    //                for (Attribute attrib: globalAttributes){
                        if (attrib != null && attrib.getFullName().equals(attributeName)) {
                            attributeValue = getAttributesAsString(attrib);
    //                        break;
                        }
    //                }
    //            }
            }
            return attributeValue;
        }

    private static String getAttributesAsString(Attribute attr) {
        return getAttributesAsString(attr, false);
    }

    /**
     * Return the value of a NetCDF {@code Attribute} instance as a
     * {@code String}. The {@code isUnsigned} parameter allow to handle byte
     * attributes as unsigned, in order to represent values in the range
     * [0,255].
     */
    private static String getAttributesAsString(Attribute attr,
            final boolean isUnsigned) {
        String values[] = null;
        if (attr != null) {
            final int nValues = attr.getLength();
            values = new String[nValues];
            final DataType datatype = attr.getDataType();
    
            // TODO: Improve the unsigned management
            if (datatype == DataType.BYTE) {
                if (isUnsigned)
                    for (int i = 0; i < nValues; i++) {
                        byte val = attr.getNumericValue(i).byteValue();
                        int myByte = (0x000000FF & ((int) val));
                        short anUnsignedByte = (short) myByte;
                        values[i] = Short.toString(anUnsignedByte);
                    }
                else {
                    for (int i = 0; i < nValues; i++) {
                        byte val = attr.getNumericValue(i).byteValue();
                        values[i] = Byte.toString(val);
                    }
                }
            } else if (datatype == DataType.SHORT) {
                for (int i = 0; i < nValues; i++) {
                    short val = attr.getNumericValue(i).shortValue();
                    values[i] = Short.toString(val);
                }
            } else if (datatype == DataType.INT) {
                for (int i = 0; i < nValues; i++) {
                    int val = attr.getNumericValue(i).intValue();
                    values[i] = Integer.toString(val);
                }
            } else if (datatype == DataType.LONG) {
                for (int i = 0; i < nValues; i++) {
                    long val = attr.getNumericValue(i).longValue();
                    values[i] = Long.toString(val);
                }
            } else if (datatype == DataType.DOUBLE) {
                for (int i = 0; i < nValues; i++) {
                    double val = attr.getNumericValue(i).doubleValue();
                    values[i] = Double.toString(val);
                }
            } else if (datatype == DataType.FLOAT) {
                for (int i = 0; i < nValues; i++) {
                    float val = attr.getNumericValue(i).floatValue();
                    values[i] = Float.toString(val);
                }
    
            } else if (datatype == DataType.STRING) {
                for (int i = 0; i < nValues; i++) {
                    values[i] = attr.getStringValue(i);
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.warning("Unhandled Attribute datatype "
                            + attr.getDataType().getClassType().toString());
            }
        }
        String value = "";
        if (values != null) {
            StringBuffer sb = new StringBuffer();
            int j = 0;
            for (; j < values.length - 1; j++) {
                sb.append(values[j]).append(",");
            }
            sb.append(values[j]);
            value = sb.toString();
        }
        return value;
    }

    private static String getAttributesAsString(final Variable var, final String attributeName) {
        String value = "";
        if (var != null){
        	Attribute attribute = var.findAttribute(attributeName);
        	if (attribute != null)
        		value = getAttributesAsString(attribute, false); 
        	
        }
    	return value;
    }

    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger.getLogger(UnusedUtilities.class.toString());

    /**
     * Gets the name, as the "description", "title" or "standard name" attribute
     * if possible, or as the variable name otherwise.
     */
    private static String getName( final Variable variable ) {
        String name = variable.getDescription();
        if (name == null || (name = name.trim()).length() == 0) {
            name = variable.getFullName();
        }
        return name;
    }

}
