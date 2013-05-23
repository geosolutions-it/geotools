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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.geotools.coverage.io.util.DateRangeTreeSet;
import org.geotools.util.DateRange;

import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;

/**
 * @author User
 *
 */
public class UnidataTimeUtilities {
    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger.getLogger(UnidataTimeUtilities.class.toString());

    @Deprecated // should die soon
    static class TimeBuilder {
        public TimeBuilder(CoordinateAxis1D axis) {
            axis1D = axis;
            values = axis1D.getCoordValues();
            units = axis.getUnitsString();
            /*
             * Gets the axis origin. In the particular case of time axis, units are typically written in the form "days since 1990-01-01
             * 00:00:00". We extract the part before "since" as the units and the part after "since" as the date.
             */
            origin = null;
            final String[] unitsParts = units.split("(?i)\\s+since\\s+");
            if (unitsParts.length == 2) {
                units = unitsParts[0].trim();
                origin = unitsParts[1].trim();
            } else {
                final Attribute attribute = axis.findAttribute("time_origin");
                if (attribute != null) {
                    origin = attribute.getStringValue();
                }
            }
            if (origin != null) {
                origin = UnidataTimeUtilities.trimFractionalPart(origin);
                // add 0 digits if absent
                origin = UnidataTimeUtilities.checkDateDigits(origin);
    
                try {
                    epoch = (Date) UnidataUtilities.getAxisFormat(AxisType.Time, origin)
                            .parseObject(origin);
                } catch (ParseException e) {
                    LOGGER.warning("Error while parsing time Axis. Skip setting the TemporalExtent from coordinateAxis");
                }
            }
        }
    
        String units;
    
        String origin;
    
        Date epoch;
    
        CoordinateAxis1D axis1D;
    
        double[] values = null;

        public static final int JGREG = 15 + 31 * (10 + 12 * 1582);
    
        public Date buildTime(int timeIndex) {
            if (epoch != null) {
                Calendar cal = new GregorianCalendar();
                cal.setTime(epoch);
                int vi = (int) Math.floor(values[timeIndex]);
                double vd = values[timeIndex] - vi;
                cal.add(UnidataTimeUtilities.getTimeUnits(units, null), vi);
                if (vd != 0.0) {
                    cal.add(UnidataTimeUtilities.getTimeUnits(units, vd), UnidataTimeUtilities.getTimeSubUnitsValue(units, vd));
                }
                return cal.getTime();
            }
            return null;
    
        }
    
        public int getNumTimes() {
            return values.length;
        }
    }

    public static final int JGREG = 15 + 31 * (10 + 12 * 1582);


    /**
     * 
     */
    private UnidataTimeUtilities() {
    }

    /**
     * @param origin
     */
    public static String checkDateDigits( String origin ) {
        String digitsCheckedOrigin = "";
        if (origin.indexOf("-") > 0) {
            String tmp = (origin.indexOf(" ") > 0 ? origin.substring(0, origin.indexOf(" ")) : origin);
            String[] originDateParts = tmp.split("-");
            for( int l = 0; l < originDateParts.length; l++ ) {
                String datePart = originDateParts[l];
                while( datePart.length() % 2 != 0 ) {
                    datePart = "0" + datePart;
                }
    
                digitsCheckedOrigin += datePart;
                digitsCheckedOrigin += (l < (originDateParts.length - 1) ? "-" : "");
            }
        }
    
        if (origin.indexOf(":") > 0) {
            digitsCheckedOrigin += " ";
            String tmp = (origin.indexOf(" ") > 0 ? origin.substring(origin.indexOf(" ") + 1) : origin);
            String[] originDateParts = tmp.split(":");
            for( int l = 0; l < originDateParts.length; l++ ) {
                String datePart = originDateParts[l];
                while( datePart.length() % 2 != 0 ) {
                    datePart = "0" + datePart;
                }
    
                digitsCheckedOrigin += datePart;
                digitsCheckedOrigin += (l < (originDateParts.length - 1) ? ":" : "");
            }
        }
    
        if (digitsCheckedOrigin.length() > 0)
            return digitsCheckedOrigin;
    
        return origin;
    }

    public static GregorianCalendar fromJulian( double injulian ) {
        int jalpha, ja, jb, jc, jd, je, year, month, day;
        // double julian = injulian + HALFSECOND / 86400.0;
        ja = (int) injulian;
        if (ja >= JGREG) {
            jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
            ja = ja + 1 + jalpha - jalpha / 4;
        }
    
        jb = ja + 1524;
        jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
        jd = 365 * jc + jc / 4;
        je = (int) ((jb - jd) / 30.6001);
        day = jb - jd - (int) (30.6001 * je);
        month = je - 1;
        if (month > 12)
            month = month - 12;
        year = jc - 4715;
        if (month > 2)
            year--;
        if (year <= 0)
            year--;
    
        // Calendar Months are 0 based
        return new GregorianCalendar(year, month - 1, day);
    }

    /**
     * 
     */
    public static int getTimeSubUnitsValue( String units, Double vd ) {
        if ("days".equalsIgnoreCase(units)) {
            int subUnit = getTimeUnits(units, vd);
            if (subUnit == Calendar.HOUR) {
                double hours = vd * 24;
                return (int) hours;
            }
    
            if (subUnit == Calendar.MINUTE) {
                double hours = vd * 24 * 60;
                return (int) hours;
            }
    
            if (subUnit == Calendar.SECOND) {
                double hours = vd * 24 * 60 * 60;
                return (int) hours;
            }
    
            if (subUnit == Calendar.MILLISECOND) {
                double hours = vd * 24 * 60 * 60 * 1000;
                return (int) hours;
            }
    
            return 0;
        }
    
        if ("hours".equalsIgnoreCase(units) || "hour".equalsIgnoreCase(units)) {
            int subUnit = getTimeUnits(units, vd);
            if (subUnit == Calendar.MINUTE) {
                double hours = vd * 24 * 60;
                return (int) hours;
            }
    
            if (subUnit == Calendar.SECOND) {
                double hours = vd * 24 * 60 * 60;
                return (int) hours;
            }
    
            if (subUnit == Calendar.MILLISECOND) {
                double hours = vd * 24 * 60 * 60 * 1000;
                return (int) hours;
            }
    
            return 0;
        }
    
        if ("minutes".equalsIgnoreCase(units)) {
            int subUnit = getTimeUnits(units, vd);
            if (subUnit == Calendar.SECOND) {
                double hours = vd * 24 * 60 * 60;
                return (int) hours;
            }
    
            if (subUnit == Calendar.MILLISECOND) {
                double hours = vd * 24 * 60 * 60 * 1000;
                return (int) hours;
            }
    
            return 0;
        }
    
        if ("seconds".equalsIgnoreCase(units)) {
            int subUnit = getTimeUnits(units, vd);
            if (subUnit == Calendar.MILLISECOND) {
                double hours = vd * 24 * 60 * 60 * 1000;
                return (int) hours;
            }
    
            return 0;
        }
    
        return 0;
    }

    /**
     * Converts NetCDF time units into opportune Calendar ones.
     * 
     * @param units
     *                {@link String}
     * @param d
     * @return int
     */
    public static int getTimeUnits( String units, Double vd ) {
        if ("months".equalsIgnoreCase(units)) {
            if (vd == null || vd == 0.0)
                // if no day, it is the first day
                return 1;
            else {
                // TODO: FIXME
            }
        } else if ("days".equalsIgnoreCase(units)) {
            if (vd == null || vd == 0.0)
                return Calendar.DATE;
            else {
                double hours = vd * 24;
                if (hours - Math.floor(hours) == 0.0)
                    return Calendar.HOUR;
    
                double minutes = vd * 24 * 60;
                if (minutes - Math.floor(minutes) == 0.0)
                    return Calendar.MINUTE;
    
                double seconds = vd * 24 * 60 * 60;
                if (seconds - Math.floor(seconds) == 0.0)
                    return Calendar.SECOND;
    
                return Calendar.MILLISECOND;
            }
        }
        if ("hours".equalsIgnoreCase(units) || "hour".equalsIgnoreCase(units)) {
            if (vd == null || vd == 0.0)
                return Calendar.HOUR;
            else {
                double minutes = vd * 24 * 60;
                if (minutes - Math.floor(minutes) == 0.0)
                    return Calendar.MINUTE;
    
                double seconds = vd * 24 * 60 * 60;
                if (seconds - Math.floor(seconds) == 0.0)
                    return Calendar.SECOND;
    
                return Calendar.MILLISECOND;
            }
        }
        if ("minutes".equalsIgnoreCase(units)) {
            if (vd == null || vd == 0.0)
                return Calendar.MINUTE;
            else {
                double seconds = vd * 24 * 60 * 60;
                if (seconds - Math.floor(seconds) == 0.0)
                    return Calendar.SECOND;
    
                return Calendar.MILLISECOND;
            }
        }
        if ("seconds".equalsIgnoreCase(units)) {
            if (vd == null || vd == 0.0)
                return Calendar.SECOND;
            else {
                return Calendar.MILLISECOND;
            }
        }
    
        return -1;
    }

    /** Return the timeIndex-th value of the time dimension of the specified variable, as a Date, or null in case that
     * variable doesn't have a time axis.
     * 
     * @param unidataReader the reader to be used for that search
     * @param variable the variable to be accessed
     * @param timeIndex the requested index
     * @param cs the coordinateSystem to be scan
     * @return
     */
    public static Date getTimeValueByIndex( final UnidataImageReader unidataReader, Variable variable, int timeIndex,
            final CoordinateSystem cs ) {
    
        if (cs != null && cs.hasTimeAxis()) {
            final int rank = variable.getRank();
            final Dimension temporalDimension = variable.getDimension(rank
                    - ((cs.hasVerticalAxis() ? UnidataUtilities.Z_DIMENSION : 2) + 1));
            return (Date) unidataReader.coordinatesVariables.get(temporalDimension.getFullName()).read(timeIndex);
        }
    
        return null;
    }

    /**
     * Return the temporal extent related to that coordinateAxis-
     * @param axis
     * @return
     */
    private static DateRange getTemporalExtent(CoordinateAxis axis) {
        if (axis == null || !AxisType.Time.equals(axis.getAxisType())) {
            throw new IllegalArgumentException("The specified axis is not a time axis");
        }
        TimeBuilder timeBuilder = new TimeBuilder((CoordinateAxis1D)axis);
        Date startTime = timeBuilder.buildTime(0);
        Date endTime = timeBuilder.buildTime(timeBuilder.getNumTimes() - 1);
        return new DateRange(startTime, endTime);
    }

    /**
     * Return the full temporal extent set related to that coordinateAxis-
     * @param axis
     * @return
     */    
    private static SortedSet<DateRange> getTemporalExtentSet(CoordinateAxis axis) {
        if (axis == null || !AxisType.Time.equals(axis.getAxisType())) {
            throw new IllegalArgumentException("The specified axis is not a time axis");
        }
    
        TimeBuilder timeBuilder = new TimeBuilder((CoordinateAxis1D)axis);
        SortedSet<DateRange> sorted = new DateRangeTreeSet();
        final int numTimes = timeBuilder.getNumTimes();
        for (int i = 0; i < numTimes; i++) {
            Date startTime = timeBuilder.buildTime(i);
            sorted.add(new DateRange(startTime, startTime));
        }
        return sorted;
    }

    /**
     * Utility method to retrieve the t-index of a Variable coverageDescriptor stored on
     * {@link NetCDFImageReader} NetCDF Flat Reader {@link HashMap} indexMap.
     * 
     * @param var
     *                {@link Variable}
     * @param range
     *                {@link Range}
     * @param imageIndex
     *                {@link int}
     * 
     * @return t-index {@link int} -1 if variable rank > 4
     */
    public static int getTIndex(Variable var, Range range, int imageIndex) {
        final int rank = var.getRank();
    
        if (rank > 2) {
            if (rank == 3) {
                return (imageIndex - range.first());
            } else {
                // return (imageIndex - range.first()) % var.getDimension(rank -
                // (Z_DIMENSION + 1)).getLength();
                return (int) Math.ceil((imageIndex - range.first())
                        / UnidataUtilities.getZDimensionLength(var));
            }
        }
    
        return -1;
    }

    /**
     * 
     * @param value
     * @return
     */
    public static String trimFractionalPart(String value) {
        value = value.trim();
        for (int i = value.length(); --i >= 0;) {
            switch (value.charAt(i)) {
            case '0':
                continue;
            case '.':
                return value.substring(0, i);
            default:
                return value;
            }
        }
        return value;
    }

}
