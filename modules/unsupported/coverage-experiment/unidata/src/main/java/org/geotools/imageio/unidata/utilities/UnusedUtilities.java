///*
// *    GeoTools - The Open Source Java GIS Toolkit
// *    http://geotools.org
// *
// *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
// *
// *    This library is free software; you can redistribute it and/or
// *    modify it under the terms of the GNU Lesser General Public
// *    License as published by the Free Software Foundation;
// *    version 2.1 of the License.
// *
// *    This library is distributed in the hope that it will be useful,
// *    but WITHOUT ANY WARRANTY; without even the implied warranty of
// *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// *    Lesser General Public License for more details.
// */
//package org.geotools.imageio.unidata.utilities;
//
//import java.io.IOException;
//import java.text.ParseException;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.List;
//import java.util.Map;
//import java.util.SortedSet;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import org.geotools.coverage.io.util.DateRangeTreeSet;
//import org.geotools.imageio.unidata.UnidataUtilities;
//import org.geotools.util.DateRange;
//
//import ucar.ma2.DataType;
//import ucar.nc2.Attribute;
//import ucar.nc2.Variable;
//import ucar.nc2.constants.AxisType;
//import ucar.nc2.dataset.CoordinateAxis;
//import ucar.nc2.dataset.CoordinateAxis1D;
//import ucar.nc2.dataset.NetcdfDataset;
//
///**
// * @author User
// *
// */
//@Deprecated // methods in this class might die soon
//abstract public class UnusedUtilities {
//
//    private static class KeyValuePair implements Map.Entry<String, String> {
//    
//        public KeyValuePair(final String key, final String value) {
//            this.key = key;
//            this.value = value;
//        }
//    
//        private String key;
//    
//        private String value;
//    
//        public String getKey() {
//            return key;
//        }
//    
//        public String getValue() {
//            return value;
//        }
//    
//        private boolean equal(Object a, Object b) {
//            return a == b || a != null && a.equals(b);
//        }
//    
//        public boolean equals(Object o) {
//            return o instanceof KeyValuePair && equal(((KeyValuePair) o).key, key)
//                    && equal(((KeyValuePair) o).value, value);
//        }
//    
//        private static int hashCode(Object a) {
//            return a == null ? 42 : a.hashCode();
//        }
//    
//        public int hashCode() {
//            return hashCode(key) * 3 + hashCode(value);
//        }
//    
//        public String toString() {
//            return "(" + key + "," + value + ")";
//        }
//    
//        public String setValue(String value) {
//            this.value = value;
//            return value;
//        }
//    }
//
//    @Deprecated // should die soon
//    private static class TimeBuilder {
//        public TimeBuilder(CoordinateAxis1D axis) {
//            axis1D = axis;
//            values = axis1D.getCoordValues();
//            units = axis.getUnitsString();
//            /*
//             * Gets the axis origin. In the particular case of time axis, units are typically written in the form "days since 1990-01-01
//             * 00:00:00". We extract the part before "since" as the units and the part after "since" as the date.
//             */
//            origin = null;
//            final String[] unitsParts = units.split("(?i)\\s+since\\s+");
//            if (unitsParts.length == 2) {
//                units = unitsParts[0].trim();
//                origin = unitsParts[1].trim();
//            } else {
//                final Attribute attribute = axis.findAttribute("time_origin");
//                if (attribute != null) {
//                    origin = attribute.getStringValue();
//                }
//            }
//            if (origin != null) {
//                origin = UnidataTimeUtilities.trimFractionalPart(origin);
//                // add 0 digits if absent
//                origin = UnidataTimeUtilities.checkDateDigits(origin);
//    
//                try {
//                    epoch = (Date) UnidataUtilities.getAxisFormat(AxisType.Time, origin)
//                            .parseObject(origin);
//                } catch (ParseException e) {
//                    LOGGER.warning("Error while parsing time Axis. Skip setting the TemporalExtent from coordinateAxis");
//                }
//            }
//        }
//    
//        String units;
//    
//        String origin;
//    
//        Date epoch;
//    
//        CoordinateAxis1D axis1D;
//    
//        double[] values = null;
//    
//        public static final int JGREG = 15 + 31 * (10 + 12 * 1582);
//    
//        public Date buildTime(int timeIndex) {
//            if (epoch != null) {
//                Calendar cal = new GregorianCalendar();
//                cal.setTime(epoch);
//                int vi = (int) Math.floor(values[timeIndex]);
//                double vd = values[timeIndex] - vi;
//                cal.add(UnidataTimeUtilities.getTimeUnits(units, null), vi);
//                if (vd != 0.0) {
//                    cal.add(UnidataTimeUtilities.getTimeUnits(units, vd), UnidataTimeUtilities.getTimeSubUnitsValue(units, vd));
//                }
//                return cal.getTime();
//            }
//            return null;
//    
//        }
//    
//        public int getNumTimes() {
//            return values.length;
//        }
//    }
//
//    private static KeyValuePair getGlobalAttribute(final NetcdfDataset dataset, final int attributeIndex) throws IOException {
//    	KeyValuePair attributePair = null;
//        if (dataset != null) {
//        	final List<Attribute> globalAttributes = dataset.getGlobalAttributes();
//            if (globalAttributes != null && !globalAttributes.isEmpty()) {
//            	final Attribute attribute = (Attribute) globalAttributes.get(attributeIndex);
//                if (attribute != null) {
//                    attributePair = new KeyValuePair(attribute.getFullName(), getAttributesAsString(attribute));
//                }
//            }
//        }
//        return attributePair;
//    }
//
//    private static KeyValuePair getAttribute(final Variable var, final int attributeIndex) {
//    	KeyValuePair attributePair = null;
//    	if (var != null){
//    		final List<Attribute> attributes = var.getAttributes();
//    	    if (attributes != null && !attributes.isEmpty()) {
//    	    	final Attribute attribute = (Attribute) attributes.get(attributeIndex);
//    	        if (attribute != null) {
//    	            attributePair = new KeyValuePair(attribute.getFullName(),getAttributesAsString(attribute));
//    	        }
//    	    }
//    	}
//        return attributePair;
//    }
//
//    /**
//         * Return a global attribute as a {@code String}. The required global
//         * attribute is specified by name
//         * 
//         * @param attributeName
//         *                the name of the required attribute.
//         * @return the value of the required attribute. Returns an empty String in
//         *         case the required attribute is not found.
//         */
//    private static String getGlobalAttributeAsString(final NetcdfDataset dataset, final String attributeName) {
//            String attributeValue = "";
//            if (dataset != null) {
//            	final Attribute attrib = dataset.findGlobalAttribute(attributeName);
//    //        	final List<Attribute> globalAttributes = dataset.getGlobalAttributes();
//    //            if (globalAttributes != null && !globalAttributes.isEmpty()) {
//    //                for (Attribute attrib: globalAttributes){
//                        if (attrib != null && attrib.getFullName().equals(attributeName)) {
//                            attributeValue = getAttributesAsString(attrib);
//    //                        break;
//                        }
//    //                }
//    //            }
//            }
//            return attributeValue;
//        }
//
//    private static String getAttributesAsString(Attribute attr) {
//        return getAttributesAsString(attr, false);
//    }
//
//    /**
//     * Return the value of a NetCDF {@code Attribute} instance as a
//     * {@code String}. The {@code isUnsigned} parameter allow to handle byte
//     * attributes as unsigned, in order to represent values in the range
//     * [0,255].
//     */
//    private static String getAttributesAsString(Attribute attr,
//            final boolean isUnsigned) {
//        String values[] = null;
//        if (attr != null) {
//            final int nValues = attr.getLength();
//            values = new String[nValues];
//            final DataType datatype = attr.getDataType();
//    
//            // TODO: Improve the unsigned management
//            if (datatype == DataType.BYTE) {
//                if (isUnsigned)
//                    for (int i = 0; i < nValues; i++) {
//                        byte val = attr.getNumericValue(i).byteValue();
//                        int myByte = (0x000000FF & ((int) val));
//                        short anUnsignedByte = (short) myByte;
//                        values[i] = Short.toString(anUnsignedByte);
//                    }
//                else {
//                    for (int i = 0; i < nValues; i++) {
//                        byte val = attr.getNumericValue(i).byteValue();
//                        values[i] = Byte.toString(val);
//                    }
//                }
//            } else if (datatype == DataType.SHORT) {
//                for (int i = 0; i < nValues; i++) {
//                    short val = attr.getNumericValue(i).shortValue();
//                    values[i] = Short.toString(val);
//                }
//            } else if (datatype == DataType.INT) {
//                for (int i = 0; i < nValues; i++) {
//                    int val = attr.getNumericValue(i).intValue();
//                    values[i] = Integer.toString(val);
//                }
//            } else if (datatype == DataType.LONG) {
//                for (int i = 0; i < nValues; i++) {
//                    long val = attr.getNumericValue(i).longValue();
//                    values[i] = Long.toString(val);
//                }
//            } else if (datatype == DataType.DOUBLE) {
//                for (int i = 0; i < nValues; i++) {
//                    double val = attr.getNumericValue(i).doubleValue();
//                    values[i] = Double.toString(val);
//                }
//            } else if (datatype == DataType.FLOAT) {
//                for (int i = 0; i < nValues; i++) {
//                    float val = attr.getNumericValue(i).floatValue();
//                    values[i] = Float.toString(val);
//                }
//    
//            } else if (datatype == DataType.STRING) {
//                for (int i = 0; i < nValues; i++) {
//                    values[i] = attr.getStringValue(i);
//                }
//            } else {
//                if (LOGGER.isLoggable(Level.WARNING))
//                    LOGGER.warning("Unhandled Attribute datatype "
//                            + attr.getDataType().getClassType().toString());
//            }
//        }
//        String value = "";
//        if (values != null) {
//            StringBuffer sb = new StringBuffer();
//            int j = 0;
//            for (; j < values.length - 1; j++) {
//                sb.append(values[j]).append(",");
//            }
//            sb.append(values[j]);
//            value = sb.toString();
//        }
//        return value;
//    }
//
//    private static String getAttributesAsString(final Variable var, final String attributeName) {
//        String value = "";
//        if (var != null){
//        	Attribute attribute = var.findAttribute(attributeName);
//        	if (attribute != null)
//        		value = getAttributesAsString(attribute, false); 
//        	
//        }
//    	return value;
//    }
//
//    /** The LOGGER for this class. */
//    private static final Logger LOGGER = Logger.getLogger(UnusedUtilities.class.toString());
//
//    /**
//     * Gets the name, as the "description", "title" or "standard name" attribute
//     * if possible, or as the variable name otherwise.
//     */
//    private static String getName( final Variable variable ) {
//        String name = variable.getDescription();
//        if (name == null || (name = name.trim()).length() == 0) {
//            name = variable.getFullName();
//        }
//        return name;
//    }
//
//    /**
//     * Return the temporal extent related to that coordinateAxis-
//     * @param axis
//     * @return
//     */
//    private static DateRange getTemporalExtent(CoordinateAxis axis) {
//        if (axis == null || !AxisType.Time.equals(axis.getAxisType())) {
//            throw new IllegalArgumentException("The specified axis is not a time axis");
//        }
//        TimeBuilder timeBuilder = new TimeBuilder((CoordinateAxis1D)axis);
//        Date startTime = timeBuilder.buildTime(0);
//        Date endTime = timeBuilder.buildTime(timeBuilder.getNumTimes() - 1);
//        return new DateRange(startTime, endTime);
//    }
//
//    /**
//     * Return the full temporal extent set related to that coordinateAxis-
//     * @param axis
//     * @return
//     */    
//    private static SortedSet<DateRange> getTemporalExtentSet(CoordinateAxis axis) {
//        if (axis == null || !AxisType.Time.equals(axis.getAxisType())) {
//            throw new IllegalArgumentException("The specified axis is not a time axis");
//        }
//    
//        TimeBuilder timeBuilder = new TimeBuilder((CoordinateAxis1D)axis);
//        SortedSet<DateRange> sorted = new DateRangeTreeSet();
//        final int numTimes = timeBuilder.getNumTimes();
//        for (int i = 0; i < numTimes; i++) {
//            Date startTime = timeBuilder.buildTime(i);
//            sorted.add(new DateRange(startTime, startTime));
//        }
//        return sorted;
//    }
//
//}
