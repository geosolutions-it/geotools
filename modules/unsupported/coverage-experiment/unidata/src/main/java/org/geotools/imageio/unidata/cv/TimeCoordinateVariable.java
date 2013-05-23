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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.imageio.unidata.UnidataTimeUtilities;
import org.geotools.imageio.unidata.UnidataUtilities;

import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * @author User
 *TODO caching
 */
class TimeCoordinateVariable extends CoordinateVariable<Date> {
    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger.getLogger(TimeCoordinateVariable.class.toString());

    private static class TimeBuilder {
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

    private final TimeBuilder timeBuilder;

    /**
     * @param binding
     * @param coordinateAxis
     */
    public TimeCoordinateVariable(CoordinateAxis1D coordinateAxis) {
        super(Date.class, coordinateAxis);
        this.timeBuilder= new TimeBuilder(coordinateAxis);
    }

    @Override
    public Date getMinimum() throws IOException {
        return timeBuilder.buildTime(0);
    }

    @Override
    public Date getMaximum() throws IOException {
        return timeBuilder.buildTime(timeBuilder.getNumTimes());
    }

    @Override
    public Date read(int index) throws IndexOutOfBoundsException {
        return timeBuilder.buildTime(index);
    }

    @Override
    public List<Date> read() throws IndexOutOfBoundsException {
       final List<Date> retValue= new ArrayList<Date>();
       final int numTimes = timeBuilder.getNumTimes();
       for (int i = 0; i < numTimes; i++) {
           Date startTime = timeBuilder.buildTime(i);
           retValue.add(startTime);
       }
        return retValue;
    }

}
