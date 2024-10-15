/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2024, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.referencing.proj;

import java.lang.reflect.Array;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.geotools.api.metadata.Identifier;
import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.IdentifiedObject;
import org.geotools.api.referencing.datum.Datum;
import org.geotools.api.referencing.datum.Ellipsoid;
import org.geotools.api.referencing.datum.PrimeMeridian;
import org.geotools.api.util.GenericName;
import org.geotools.metadata.i18n.ErrorKeys;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.metadata.math.XMath;
import org.geotools.referencing.operation.DefaultOperationMethod;
import si.uom.SI;
import tech.units.indriya.AbstractUnit;

/**
 * A Formatter that formats {@link IdentifiedObject} objects as PROJ strings. Supported
 * IdentifiedObjects need to be {@link PROJFormattable}.
 *
 * <p>Call toPROJ(identifiedObject) to get the associated proj String.
 */
public class PROJFormatter {

    private StringBuffer buffer;

    private Citation authority = Citations.PROJ;

    /** The unit for formatting measures, or {@code null} for the "natural" unit. */
    private Unit<Length> linearUnit;

    /**
     * The unit for formatting measures, or {@code null} for the "natural" unit of each element.
     * This value is set for example by "GEOGCS", which force its enclosing "PRIMEM" to take the
     * same units than itself.
     */
    private Unit<Angle> angularUnit;

    private final FieldPosition dummy = new FieldPosition(0);

    private static final PROJAliases PROJ_ALIASES = new PROJAliases();

    private static final PROJRefiner PROJ_REFINER = new PROJRefiner();

    private boolean projectedCRS = false;

    private boolean datumProvided = false;

    private boolean ellipsoidProvided = false;

    private boolean primeMeridianProvided = false;

    /** Creates a new instance of the PROJFormatter. */
    public PROJFormatter() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(17);
        buffer = new StringBuffer();
    }

    /** The object to use for formatting numbers. */
    private NumberFormat numberFormat;

    public boolean isProjectedCRS() {
        return projectedCRS;
    }

    public boolean isDatumProvided() {
        return datumProvided;
    }

    public boolean isEllipsoidProvided() {
        return ellipsoidProvided;
    }

    public boolean isPrimeMeridianProvided() {
        return primeMeridianProvided;
    }

    public void append(final PROJFormattable formattable) {
        int base = buffer.length();
        final IdentifiedObject info =
                (formattable instanceof IdentifiedObject) ? (IdentifiedObject) formattable : null;
        if (info != null) {
            buffer.append(getName(info)).append(" ");
        }

        String keyword = formattable.formatPROJ(this);
        buffer.insert(base, keyword);
    }

    public void append(final GeneralParameterValue parameter) {
        if (parameter instanceof ParameterValueGroup) {
            for (final GeneralParameterValue param : ((ParameterValueGroup) parameter).values()) {
                append(param);
            }
        }
        if (parameter instanceof ParameterValue) {
            final ParameterValue<?> param = (ParameterValue) parameter;
            final ParameterDescriptor<?> descriptor = param.getDescriptor();
            final Unit<?> valueUnit = descriptor.getUnit();
            Unit<?> unit = valueUnit;

            if (unit != null && !AbstractUnit.ONE.equals(unit)) {
                if (linearUnit != null && unit.isCompatible(linearUnit)) {
                    unit = linearUnit;
                } else if (angularUnit != null && unit.isCompatible(angularUnit)) {
                    unit = angularUnit;
                }
            }
            String name = getName(descriptor);
            if (name != null && !name.isBlank()) {
                buffer.append("+" + name);
                if (unit != null) {
                    double value;
                    try {
                        value = param.doubleValue();
                    } catch (IllegalStateException exception) {
                        // May happen if a parameter is mandatory (e.g. "semi-major")
                        // but no value has been set for this parameter.
                        value = Double.NaN;
                    }
                    if (!unit.equals(valueUnit)) {
                        value = XMath.trimDecimalFractionDigits(value, 4, 9);
                    }
                    buffer.append("=");
                    format(value);
                    buffer.append(" ");
                } else {
                    buffer.append("=");
                    appendObject(param.getValue());
                    buffer.append(" ");
                }
            }
        }
    }

    public void append(final Unit<?> unit) {
        if (unit != null) {
            buffer.append("+units=");
            String symbol = unit.getSymbol();
            if (symbol == null) {
                symbol = unit.toString();
            }
            buffer.append(symbol);
        }
    }

    /**
     * Append the specified value to a string buffer. If the value is an array, then the array
     * elements are appended recursively (i.e. the array may contains sub-array).
     */
    private void appendObject(final Object value) {
        if (value == null) {
            buffer.append("null");
            return;
        }
        if (value.getClass().isArray()) {
            buffer.append('{');
            final int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    buffer.append(',').append(' ');
                }
                appendObject(Array.get(value, i));
            }
            buffer.append('}');
            return;
        }
        if (value instanceof Number) {
            format((Number) value);
        } else {
            buffer.append('"').append(value).append('"');
        }
    }

    public void append(String value) {
        buffer.append(value);
    }

    public void append(final int number) {
        format(number);
    }

    public void append(final double number) {
        format(number);
    }

    /**
     * Returns the preferred name for the specified object. If the specified object contains a name
     * from the preferred authority then this name is returned. Otherwise, it will be added to not
     * parseable list
     *
     * @param info The object to looks for a preferred name.
     * @return The preferred name.
     */
    public String getName(final IdentifiedObject info) {
        final Identifier name = info.getName();
        if (Citations.PROJ != name.getAuthority()) {
            final Collection<GenericName> aliases = info.getAlias();
            if (aliases != null && !aliases.isEmpty()) {
                /*
                 * The main name doesn't matches. Search in alias. We will first
                 * check if alias implements Identifier (this is the case of
                 * Geotools implementation). Otherwise, we will look at the
                 * scope in generic name.
                 */
                for (final GenericName alias : aliases) {
                    if (alias instanceof Identifier) {
                        final Identifier candidate = (Identifier) alias;
                        if (Citations.PROJ == candidate.getAuthority()) {
                            if (info instanceof DefaultOperationMethod) {
                                projectedCRS = true;
                            }
                            return candidate.getCode();
                        }
                    }
                }
                // The "null" locale argument is required for getting the unlocalized version.
                final String title = authority.getTitle().toString(null);
                for (final GenericName alias : aliases) {
                    final GenericName scope = alias.scope().name();
                    if (scope != null) {
                        if (title.equalsIgnoreCase(scope.toString())) {
                            if (info instanceof Datum) {
                                datumProvided = true;
                            } else if (info instanceof Ellipsoid) {
                                ellipsoidProvided = true;
                            }
                            return alias.tip().toString();
                        }
                    }
                }
            }
            if (info instanceof Ellipsoid) {
                String projAlias = PROJ_ALIASES.getEllipsoidAlias(info.getName().getCode());
                if (projAlias != null) {
                    ellipsoidProvided = true;
                    return projAlias;
                }
            }
            if (info instanceof PrimeMeridian) {
                String projAlias = PROJ_ALIASES.getPrimeMeridianAlias(info.getName().getCode());
                if (projAlias != null) {
                    primeMeridianProvided = true;
                    return projAlias;
                }
            }
        }
        return "";
    }

    public Identifier getIdentifier(final IdentifiedObject info) {
        Identifier first = null;
        if (info != null) {
            final Collection<? extends Identifier> identifiers = info.getIdentifiers();
            if (identifiers != null) {
                for (final Identifier id : identifiers) {
                    if (authority == id.getAuthority()) {
                        return id;
                    }
                    if (first == null) {
                        first = id;
                    }
                }
            }
        }
        return first;
    }

    public void clear() {
        if (buffer != null) {
            buffer.setLength(0);
        }
        linearUnit = null;
        angularUnit = null;
        datumProvided = false;
        ellipsoidProvided = false;
        primeMeridianProvided = false;
        projectedCRS = false;
    }

    /**
     * The linear unit for formatting measures, or {@code null} for the "natural" unit of each WKT
     * element.
     *
     * @return The unit for measure. Default value is {@code null}.
     */
    public Unit<Length> getLinearUnit() {
        return linearUnit;
    }

    /**
     * Set the unit for formatting linear measures.
     *
     * @param unit The new unit, or {@code null}.
     */
    public void setLinearUnit(final Unit<Length> unit) {
        if (unit != null && !SI.METRE.isCompatible(unit)) {
            throw new IllegalArgumentException(
                    MessageFormat.format(ErrorKeys.NON_LINEAR_UNIT_$1, unit));
        }
        linearUnit = unit;
    }

    /**
     * The angular unit for formatting measures, or {@code null} for the "natural" unit of each WKT
     * element. This value is set for example by "GEOGCS", which force its enclosing "PRIMEM" to
     * take the same units than itself.
     *
     * @return The unit for measure. Default value is {@code null}.
     */
    public Unit<Angle> getAngularUnit() {
        return angularUnit;
    }

    /**
     * Set the angular unit for formatting measures.
     *
     * @param unit The new unit, or {@code null}.
     */
    public void setAngularUnit(final Unit<Angle> unit) {
        if (unit != null && (!SI.RADIAN.isCompatible(unit) || AbstractUnit.ONE.equals(unit))) {
            throw new IllegalArgumentException(
                    MessageFormat.format(ErrorKeys.NON_ANGULAR_UNIT_$1, unit));
        }
        angularUnit = unit;
    }

    /** Format an arbitrary number. */
    private void format(final Number number) {
        if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
            format(number.intValue());
        } else {
            format(number.doubleValue());
        }
    }

    /** Formats an integer number. */
    private void format(final int number) {
        final int fraction = numberFormat.getMinimumFractionDigits();
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.format(number, buffer, dummy);
        numberFormat.setMinimumFractionDigits(fraction);
    }

    /** Formats a floating point number. */
    private void format(final double number) {
        if (number == Math.floor(number)) {
            format((int) number);
        } else {
            numberFormat.format(number, buffer, dummy);
        }
    }

    /**
     * That's the main method to get the PROJ String out of a {@link IdentifiedObject}.
     *
     * @param identifiedObject the identified object to be formatted
     * @return
     */
    public String toPROJ(IdentifiedObject identifiedObject) {
        if (identifiedObject instanceof PROJFormattable) {
            ((PROJFormattable) identifiedObject).formatPROJ(this);
            String refinedString =
                    PROJ_REFINER.refine(
                            buffer.toString(),
                            identifiedObject.getIdentifiers().iterator().next().getCode());
            buffer = new StringBuffer(refinedString);
            return refinedString;
        } else {
            throw new UnsupportedOperationException(
                    "PROJ String is not supported for this type of object: " + identifiedObject);
        }
    }
}
