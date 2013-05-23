package org.geotools.imageio.unidata.utilities;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.imageio.Identification;
import org.geotools.imageio.unidata.UnidataUtilities;
import org.geotools.metadata.sql.MetadataException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCoordinateSystemAxis;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.geotools.referencing.datum.DefaultPrimeMeridian;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPosition;
import org.geotools.util.SimpleInternationalString;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.temporal.Position;

import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableDS;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class UnidataCRSUtilities {

    private final static java.util.logging.Logger LOGGER = Logger.getLogger("org.geotools.imageio.unidata");
    
    public static ReferencingFactoryContainer FACTORY_CONTAINER = ReferencingFactoryContainer.instance(GeoTools.getDefaultHints());
    
    final static PrecisionModel PRECISION_MODEL = new PrecisionModel(PrecisionModel.FLOATING);
    final static GeometryFactory GEOM_FACTORY = new GeometryFactory(PRECISION_MODEL);

    /**
     * Set of commonly used symbols for "seconds".
     * 
     * @todo Needs a more general way to set unit symbols once the Unit API is
     *       completed.
     */
    private static final String[] DAYS = {"day", "dd", "days since"};

    /**
     * Set of commonly used symbols for "degrees".
     * 
     * @todo Needs a more general way to set unit symbols once the Unit API is
     *       completed.
     */
    private static final String[] DEGREES = {"degree", "degrees", "deg", "°"};

    /**
     * Set of commonly used symbols for "seconds".
     * 
     * @todo Needs a more general way to set unit symbols once the Unit API is
     *       completed.
     */
    private static final String[] HOURS = {"hour", "hh", "hours since"};

    /**
     * Set of commonly used symbols for "metres".
     * 
     * @todo Needs a more general way to set unit symbols once the Unit API is
     *       completed.
     */
    private static final String[] METERS = {"meter", "meters", "metre", "metres", "m"};

    /**
     * Set of commonly used symbols for "seconds".
     * 
     * @todo Needs a more general way to set unit symbols once the Unit API is
     *       completed.
     */
    private static final String[] MINUTES = {"minute", "min", "minutes since"};

    /**
     * Set of commonly used symbols for "seconds".
     * 
     * @todo Needs a more general way to set unit symbols once the Unit API is
     *       completed.
     */
    private static final String[] SECONDS = {"second", "sec", "seconds since"};
    
    public final static Set<String> VERTICAL_AXIS_NAMES = new HashSet<String>();
    /**
     * Set of {@linkplain DefaultEllipsoid ellipsoids} already defined.
     */
    private static final DefaultEllipsoid[] ELLIPSOIDS = new DefaultEllipsoid[]{//
    DefaultEllipsoid.CLARKE_1866, //
            DefaultEllipsoid.GRS80, //
            DefaultEllipsoid.INTERNATIONAL_1924, //
            DefaultEllipsoid.SPHERE, //
            DefaultEllipsoid.WGS84//
    };

    /**
     * The mapping between UCAR axis type and ISO axis directions.
     */
    private static final Map<AxisType, String> DIRECTIONS = new HashMap<AxisType, String>(16);

    private static final Map<AxisType, String> OPPOSITES = new HashMap<AxisType, String>(16);
    static {
        add(AxisType.Time, "future", "past");
        add(AxisType.GeoX, "east", "west");
        add(AxisType.GeoY, "north", "south");
        add(AxisType.GeoZ, "up", "down");
        add(AxisType.Lat, "north", "south");
        add(AxisType.Lon, "east", "west");
        add(AxisType.Height, "up", "down");
        add(AxisType.Pressure, "up", "down");
        VERTICAL_AXIS_NAMES.add("elevation");
        VERTICAL_AXIS_NAMES.add("height");
        VERTICAL_AXIS_NAMES.add("z");
        VERTICAL_AXIS_NAMES.add("depth");
        VERTICAL_AXIS_NAMES.add("pressure");
        
    }

    /**
     * The object to use for parsing and formatting units.
     */
    private static UnitFormat unitFormat;
    
    /**
     * Adds a mapping between UCAR type and ISO direction.
     */
    private static void add( final AxisType type, final String direction, final String opposite ) {
        if (DIRECTIONS.put(type, direction) != null) {
            throw new IllegalArgumentException(String.valueOf(type));
        }

        if (OPPOSITES.put(type, opposite) != null) {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    private static String[] getUnitDirection(CoordinateAxis axis) {
    AxisType type = axis.getAxisType();
        String units = axis.getUnitsString();
        /*
         * Gets the axis direction, taking in account the possible reversal or
         * vertical axis. Note that geographic and projected
         * CoordinateReferenceSystem have the same directions. We can
         * distinguish them either using the ISO CoordinateReferenceSystem type
         * ("geographic" or "projected"), the ISO CS type ("ellipsoidal" or
         * "cartesian") or the units ("degrees" or "m").
         */
        String direction = DIRECTIONS.get(type);
        if (direction != null) {
            if (CF.POSITIVE_DOWN.equalsIgnoreCase(axis.getPositive())) {
                direction = OPPOSITES.get(type);
            }
            final int offset = units.lastIndexOf('_');
            if (offset >= 0) {
                final String unitsDirection = units.substring(offset + 1).trim();
                final String opposite = OPPOSITES.get(type);
                if (unitsDirection.equalsIgnoreCase(opposite)) {
                    // TODO WARNING: INCONSISTENT AXIS ORIENTATION
                    direction = opposite;
                }
                if (unitsDirection.equalsIgnoreCase(direction)) {
                    units = units.substring(0, offset).trim();
                }
            }
        }
        return new String[]{units, direction};
    }

    
    /**
     * Returns the {@linkplain CoordinateSystem coordinate system}. The default
     * implementation builds a coordinate system using the
     * {@linkplain #getAxis axes} defined in the metadata.
     * 
     * @throws MetadataException
     *                 if there is less than 2 axes defined in the metadata, or
     *                 if the creation of the coordinate system failed.
     * 
     * @see #getAxis
     * @see CoordinateSystem
     */
    public static CoordinateSystem getCoordinateSystem( String csName, final List<CoordinateAxis> axes ) throws Exception {
        int dimension = axes.size();

        // FIXME how to tell when baseCRS would have been null?
        final String type = UnidataMetadataUtilities.ELLIPSOIDAL;

        final CSFactory factory = FACTORY_CONTAINER.getCSFactory();
        final Map<String, String> map = Collections.singletonMap("name", csName);
        if (dimension < 2) {
            throw new MetadataException("Number of dimension error : " + dimension);
        }
        try {
            if (dimension < 3) {
                CoordinateAxis axis1 = axes.get(0);
                String[] unitDirection1 = getUnitDirection(axis1);
                CoordinateSystemAxis csAxis1 = getAxis(axis1.getName(), getDirection(unitDirection1[1]), unitDirection1[0]);
                CoordinateAxis axis2 = axes.get(1);
                String[] unitDirection2 = getUnitDirection(axis2);
                CoordinateSystemAxis csAxis2 = getAxis(axis2.getName(), getDirection(unitDirection2[1]), unitDirection2[0]);
                if (type.equalsIgnoreCase(UnidataMetadataUtilities.CARTESIAN)) {
                    return factory.createCartesianCS(map, csAxis1, csAxis2);
                }
                if (type.equalsIgnoreCase(UnidataMetadataUtilities.ELLIPSOIDAL)) {
                    return factory.createEllipsoidalCS(map, csAxis1, csAxis2);
                }
            } else {
                CoordinateAxis axis1 = axes.get(0);
                String[] unitDirection1 = getUnitDirection(axis1);
                CoordinateSystemAxis csAxis1 = getAxis(axis1.getName(), getDirection(unitDirection1[1]), unitDirection1[0]);
                CoordinateAxis axis2 = axes.get(1);
                String[] unitDirection2 = getUnitDirection(axis2);
                CoordinateSystemAxis csAxis2 = getAxis(axis2.getName(), getDirection(unitDirection2[1]), unitDirection2[0]);
                CoordinateAxis axis3 = axes.get(1);
                String[] unitDirection3 = getUnitDirection(axis3);
                CoordinateSystemAxis csAxis3 = getAxis(axis3.getName(), getDirection(unitDirection3[1]), unitDirection3[0]);

                if (type.equalsIgnoreCase(UnidataMetadataUtilities.CARTESIAN)) {
                    return factory.createCartesianCS(map, csAxis1, csAxis2, csAxis3);
                }
                if (type.equalsIgnoreCase(UnidataMetadataUtilities.ELLIPSOIDAL)) {
                    return factory.createEllipsoidalCS(map, csAxis1, csAxis2, csAxis3);
                }
            }
            /*
             * Should not happened, since the type value should be contained in
             * the {@link UnidataGeospatialMetadata#CS_TYPES} list.
             */
            throw new Exception("Coordinate system type not known : " + type);
        } catch (FactoryException e) {
            throw new Exception(e.getLocalizedMessage());
        }
    }

    
    /**
     * Get the {@link AxisDirection} object related to the specified direction
     * 
     * @param direction
     * @return
     */
    private static AxisDirection getDirection( final String direction ) {
        return AxisDirection.valueOf(direction);
    }

    /**
     * Build a proper {@link CoordinateSystemAxis} given the set composed of
     * axisName, axisDirection and axis unit of measure.
     * 
     * @param axisName
     *                the name of the axis to be built.
     * @param direction
     *                the {@linkplain AxisDirection direction} of the axis.
     * @param unitName
     *                the unit of measure string.
     * @return a proper {@link CoordinateSystemAxis} instance or {@code null} if
     *         unable to build it.
     * @throws FactoryException
     */
    private static CoordinateSystemAxis getAxis( final String axisName, final AxisDirection direction, final String unitName )
            throws FactoryException {
        if (axisName == null) {
            return null;
        }
        final DefaultCoordinateSystemAxis axisFound = DefaultCoordinateSystemAxis.getPredefined(axisName, direction);
        if (axisFound != null) {
            return axisFound;
        }

        /*
         * The current axis defined in the metadata tree is not already known in
         * the Geotools implementation, so one will build it using those
         * information.
         */
        final Unit< ? > unit = getUnit(unitName);
        final Map<String, String> map = Collections.singletonMap("name", axisName);
        try {
            return FACTORY_CONTAINER.getCSFactory().createCoordinateSystemAxis(map, axisName, direction, unit);
        } catch (FactoryException e) {
            throw new FactoryException(e.getLocalizedMessage());
        }
    }

    /**
     * Returns the datum. The default implementation performs the following
     * steps:
     * <p>
     * <ul>
     * <li>Verifies if the datum name contains {@code WGS84}, and returns a
     * {@link DefaultGeodeticDatum#WGS84} geodetic datum if it is the case.
     * </li>
     * <li>Builds a {@linkplain PrimeMeridian prime meridian} using information
     * stored into the metadata tree. </li>
     * <li>Returns a {@linkplain DefaultGeodeticDatum geodetic datum} built on
     * the prime meridian. </li>
     * </ul>
     * </p>
     * @param ellipsoidName 
     * @param semiMajorAxus 
     * @param inverseFlattening 
     * @param ellipsoidUnit 
     * @param secondDefiningParameter 
     * 
     * @throws MetadataException
     *                 if the datum is not defined, or if the
     *                 {@link #getEllipsoid} method fails.
     * 
     * @todo: The current implementation only returns a
     *        {@linkplain GeodeticDatum geodetic datum}, other kind of datum
     *        have to be generated too.
     * 
     * @see #getEllipsoid
     */
    public static Datum getDatum( String datumName, double greenwichLon, String primeMeridianName, String ellipsoidName,
            double semiMajorAxus, double inverseFlattening, String ellipsoidUnit, String secondDefiningParameter )
            throws Exception {

        if (datumName == null) {
            throw new Exception("Datum not defined.");
        }
        if (datumName.toUpperCase().contains("WGS84")) {
            return DefaultGeodeticDatum.WGS84;
        }
        final PrimeMeridian primeMeridian;
        /*
         * By default, if the prime meridian name is not defined, or if it is
         * defined with {@code Greenwich}, one chooses the {@code Greenwich}
         * meridian as prime meridian. Otherwise one builds it, using the
         * {@code greenwichLongitude} parameter.
         */
        if ((primeMeridianName == null) || (primeMeridianName != null && primeMeridianName.toLowerCase().contains("greenwich"))) {
            primeMeridian = DefaultPrimeMeridian.GREENWICH;
        } else {

            primeMeridian = (Double.isNaN(greenwichLon)) ? DefaultPrimeMeridian.GREENWICH : new DefaultPrimeMeridian(
                    primeMeridianName, greenwichLon);
        }

        Ellipsoid ellipsoid = getEllipsoid(ellipsoidName, semiMajorAxus, inverseFlattening, ellipsoidUnit,
                secondDefiningParameter);
        return new DefaultGeodeticDatum(datumName, ellipsoid, primeMeridian);
    }

    /**
     * Returns the ellipsoid. Depending on whether
     * {@link ImageReferencing#semiMinorAxis} or
     * {@link ImageReferencing#inverseFlattening} has been defined, the default
     * implementation will construct an ellispoid using
     * {@link DatumFactory#createEllipsoid} or
     * {@link DatumFactory#createFlattenedSphere} respectively.
     * 
     * @throws MetadataException
     *                 if the operation failed to create the
     *                 {@linkplain Ellipsoid ellipsoid}.
     * 
     * @see #getUnit(String)
     */
    private static Ellipsoid getEllipsoid( String ellipsoidName, double semiMajorAxis, double inverseFlattening, String ellipsoidUnit,
            String secondDefiningParameterType ) throws Exception {
        if (ellipsoidName != null) {
            for( final DefaultEllipsoid ellipsoid : ELLIPSOIDS ) {
                if (ellipsoid.nameMatches(ellipsoidName)) {
                    return ellipsoid;
                }
            }
        } else {
            throw new Exception("Ellipsoid name not defined.");
        }
        // It has a name defined, but it is not present in the list of known
        // ellipsoids.
        if (Double.isNaN(semiMajorAxis)) {
            throw new Exception("Ellipsoid semi major axis not defined.");
        }
        if (ellipsoidUnit == null) {
            throw new Exception("Ellipsoid unit not defined.");
        }
        final Unit unit = getUnit(ellipsoidUnit);
        final Map<String, String> map = Collections.singletonMap("name", ellipsoidName);
        try {
            final DatumFactory datumFactory = FACTORY_CONTAINER.getDatumFactory();
            return (secondDefiningParameterType.equals(UnidataMetadataUtilities.MD_DTM_GD_EL_SEMIMINORAXIS)) ? datumFactory
                    .createEllipsoid(map, semiMajorAxis, semiMajorAxis, unit) : datumFactory.createFlattenedSphere(map,
                    semiMajorAxis, inverseFlattening, unit);
        } catch (FactoryException e) {
            throw new Exception(e.getLocalizedMessage(),e);
        }
    }
    /**
     * Returns the unit which matches with the name given.
     * 
     * @param unitName
     *                The name of the unit. Should not be {@code null}.
     * @return The unit matching with the specified name.
     * @throws MetadataException
     *                 if the unit name does not match with the
     *                 {@linkplain #unitFormat unit format}.
     */
    private static Unit< ? > getUnit( final String unitName ) throws FactoryException {
        if (contains(unitName, METERS)) {
            return SI.METER;
        } else if (contains(unitName, DEGREES)) {
            return NonSI.DEGREE_ANGLE;
        } else if (contains(unitName, SECONDS)) {
            return SI.SECOND;
        } else if (contains(unitName, MINUTES)) {
            return NonSI.MINUTE;
        } else if (contains(unitName, HOURS)) {
            return NonSI.HOUR;
        } else if (contains(unitName, DAYS)) {
            return NonSI.DAY;
        } else {
            if (unitFormat == null) {
                unitFormat = UnitFormat.getInstance();
            }
            try {
                return (Unit< ? >) unitFormat.parseObject(unitName);
            } catch (ParseException e) {
                throw new FactoryException("Unit not known : " + unitName, e);
            }
        }
    }

    /**
     * Check if {@code toSearch} appears in the {@code list} array. Search is
     * case-insensitive. This is a temporary patch (will be removed when the
     * final API for JSR-108: Units specification will be available).
     */
    private static boolean contains( final String toSearch, final String[] list ) {
        for( int i = list.length; --i >= 0; ) {
            if (toSearch.toLowerCase().contains(list[i].toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static VerticalCRS buildVerticalCrs( ucar.nc2.dataset.CoordinateSystem cs, String csName, CoordinateAxis zAxis,
            final CSFactory csFactory, final DatumFactory datumFactory, final CRSFactory crsFactory ) {
        VerticalCRS verticalCRS = null;
        try {
            if (zAxis != null) {
                String axisName = zAxis.getFullName();
                if (!UnidataCRSUtilities.VERTICAL_AXIS_NAMES.contains(axisName)) {
                    return null;
                }
                String units = zAxis.getUnitsString();
                AxisType type = zAxis.getAxisType();

                String v_crsName = "Unknown";
                String v_datumName = "Unknown";
                String v_datumType = null;
                if (cs.getRankDomain() > 2 && cs.hasVerticalAxis()) {
                    v_datumName = new Identification("Mean Sea Level", null, null, "EPSG:5100").getName();

                    if (cs.getElevationAxis() != null || cs.getAzimuthAxis() != null || cs.getZaxis() != null)
                        v_datumType = "geoidal";
                    else if (cs.getHeightAxis() != null) {
                        CoordinateAxis axis = cs.getHeightAxis();
                        if (!axis.getName().equalsIgnoreCase("height")) {
                            v_datumType = "depth";
                            v_crsName = new Identification("mean sea level depth", null, null, "EPSG:5715").getName();
                        } else {
                            v_datumType = "geoidal";
                            v_crsName = new Identification("mean sea level height", null, null, "EPSG:5714").getName();
                        }
                    } else if (cs.getPressureAxis() != null)
                        v_datumType = "barometric";
                    else
                        v_datumType = "other_surface";
                }

                /*
                 * Gets the axis direction, taking in account the possible reversal or
                 * vertical axis. Note that geographic and projected
                 * CoordinateReferenceSystem have the same directions. We can
                 * distinguish them either using the ISO CoordinateReferenceSystem type
                 * ("geographic" or "projected"), the ISO CS type ("ellipsoidal" or
                 * "cartesian") or the units ("degrees" or "m").
                 */
                String direction = DIRECTIONS.get(type);
                if (direction != null) {
                    if (CF.POSITIVE_DOWN.equalsIgnoreCase(zAxis.getPositive())) {
                        direction = OPPOSITES.get(type);
                    }
                    final int offset = units.lastIndexOf('_');
                    if (offset >= 0) {
                        final String unitsDirection = units.substring(offset + 1).trim();
                        final String opposite = OPPOSITES.get(type);
                        if (unitsDirection.equalsIgnoreCase(opposite)) {
                            // TODO WARNING: INCONSISTENT AXIS ORIENTATION
                            direction = opposite;
                        }
                        if (unitsDirection.equalsIgnoreCase(direction)) {
                            units = units.substring(0, offset).trim();
                        }
                    }
                }
                final Map<String, String> csMap = Collections.singletonMap("name", csName);
                VerticalCS verticalCS = csFactory.createVerticalCS(csMap,
                        getAxis(zAxis.getName(), getDirection(direction), units));

                // Creating the Vertical Datum
                final Map<String, String> datumMap = Collections.singletonMap("name", v_datumName);
                final VerticalDatum verticalDatum = datumFactory.createVerticalDatum(datumMap,
                        VerticalDatumType.valueOf(v_datumType));

                final Map<String, String> crsMap = Collections.singletonMap("name", v_crsName);
                verticalCRS = crsFactory.createVerticalCRS(crsMap, verticalDatum, verticalCS);
            }
        } catch (FactoryException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Unable to parse vertical CRS", e);
            verticalCRS = null;
        }
        return verticalCRS;
    }

    public static TemporalCRS buildTemporalCrs( String t_csName, String crsName, CoordinateAxis timeAxis, final CSFactory csFactory,
            final DatumFactory datumFactory, final CRSFactory crsFactory ) {
        String t_datumName = new Identification("ISO8601", null, null, null).getName();
        TemporalCRS temporalCRS = null;
        try {
            if (timeAxis != null) {
                AxisType type = timeAxis.getAxisType();
                String units = timeAxis.getUnitsString();

                /*
                 * Gets the axis direction, taking in account the possible reversal or
                 * vertical axis. Note that geographic and projected
                 * CoordinateReferenceSystem have the same directions. We can
                 * distinguish them either using the ISO CoordinateReferenceSystem type
                 * ("geographic" or "projected"), the ISO CS type ("ellipsoidal" or
                 * "cartesian") or the units ("degrees" or "m").
                 */
                String direction = DIRECTIONS.get(type);
                if (direction != null) {
                    if (CF.POSITIVE_DOWN.equalsIgnoreCase(timeAxis.getPositive())) {
                        direction = OPPOSITES.get(type);
                    }
                    final int offset = units.lastIndexOf('_');
                    if (offset >= 0) {
                        final String unitsDirection = units.substring(offset + 1).trim();
                        final String opposite = OPPOSITES.get(type);
                        if (unitsDirection.equalsIgnoreCase(opposite)) {
                            // TODO WARNING: INCONSISTENT AXIS ORIENTATION
                            direction = opposite;
                        }
                        if (unitsDirection.equalsIgnoreCase(direction)) {
                            units = units.substring(0, offset).trim();
                        }
                    }
                }

                Date epoch = null;
                String t_originDate = null;
                if (AxisType.Time.equals(type)) {
                    String origin = null;
                    final String[] unitsParts = units.split("(?i)\\s+since\\s+");
                    if (unitsParts.length == 2) {
                        units = unitsParts[0].trim();
                        origin = unitsParts[1].trim();
                    } else {
                        final Attribute attribute = timeAxis.findAttribute("time_origin");
                        if (attribute != null) {
                            origin = attribute.getStringValue();
                        }
                    }
                    if (origin != null) {
                        origin = UnidataTimeUtilities.trimFractionalPart(origin);
                        // add 0 digits if absent
                        origin = UnidataTimeUtilities.checkDateDigits(origin);

                        try {
                            epoch = (Date) UnidataUtilities.getAxisFormat(type, origin).parseObject(origin);
                            GregorianCalendar cal = new GregorianCalendar();
                            cal.setTime(epoch);
                            DefaultInstant instant = new DefaultInstant(new DefaultPosition(cal.getTime()));
                            t_originDate = instant.getPosition().getDateTime().toString();
                        } catch (ParseException e) {
                            throw new IllegalArgumentException(e);
                            // TODO: Change the handle this exception
                        }
                    }
                }

                String axisName = timeAxis.getName();

                if (t_csName == null) {
                    t_csName = "Unknown";
                }
                final Map<String, String> csMap = Collections.singletonMap("name", t_csName);
                final TimeCS timeCS = csFactory.createTimeCS(csMap, getAxis(axisName, getDirection(direction), units));

                // Creating the Temporal Datum
                if (t_datumName == null) {
                    t_datumName = "Unknown";
                }
                final Map<String, String> datumMap = Collections.singletonMap("name", t_datumName);
                final Position timeOrigin = new DefaultPosition(new SimpleInternationalString(t_originDate));
                final TemporalDatum temporalDatum = datumFactory.createTemporalDatum(datumMap, timeOrigin.getDate());

                // Finally creating the Temporal CoordinateReferenceSystem
                if (crsName == null) {
                    crsName = "Unknown";
                }
                final Map<String, String> crsMap = Collections.singletonMap("name", crsName);
                temporalCRS = crsFactory.createTemporalCRS(crsMap, temporalDatum, timeCS);
            }
        } catch (FactoryException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Unable to parse temporal CRS", e);
            temporalCRS = null;
        } catch (ParseException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Unable to parse temporal CRS", e);
            temporalCRS = null;
        }
        return temporalCRS;
    }

    public static Geometry extractEnvelopeAsGeometry(VariableDS variable) {
        final List<ucar.nc2.dataset.CoordinateSystem> systems = variable.getCoordinateSystems();
        ucar.nc2.dataset.CoordinateSystem cs = systems.get(0);
        double[] wsen = UnidataCRSUtilities.getEnvelope(cs);
        // Currently we only support Geographic CRS
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(wsen[0], wsen[2], wsen[1], wsen[3], UnidataCRSUtilities.WGS84);
        return GEOM_FACTORY.toGeometry(referencedEnvelope);
    }
    
    public static ucar.nc2.dataset.CoordinateSystem getCoordinateSystem(VariableDS variableDS) {
        final List<ucar.nc2.dataset.CoordinateSystem> systems = variableDS.getCoordinateSystems();
        if (systems.isEmpty()) {
            throw new RuntimeException("Coordinate system for Variable " + variableDS.getFullName() + " haven't been found");
        }
        return systems.get(0);
    }

    public static double[] getEnvelope(ucar.nc2.dataset.CoordinateSystem cs ) {
        // TODO: Handle 3D GEO CoordinateReferenceSystem
        double[] envelope = null;
        if (cs != null) {
            /*
             * Adds the axis in reverse order, because the NetCDF image reader
             * put the last dimensions in the rendered image. Typical NetCDF
             * convention is to put axis in the (time, depth, latitude,
             * longitude) order, which typically maps to (longitude, latitude,
             * depth, time) order in our referencing framework.
             */
            final List<CoordinateAxis> axes = cs.getCoordinateAxes();
            envelope = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
            for( int i = axes.size(); --i >= 0; ) {
                final CoordinateAxis axis = axes.get(i);
    
                // final String name = UnidataSliceUtilities.getName(axis);
                final AxisType type = axis.getAxisType();
                // final String units = axis.getUnitsString();
    
                /*
                 * Gets the axis direction, taking in account the possible
                 * reversal or vertical axis. Note that geographic and projected
                 * CoordinateReferenceSystem have the same directions. We can
                 * distinguish them either using the ISO
                 * CoordinateReferenceSystem type ("geographic" or "projected"),
                 * the ISO CS type ("ellipsoidal" or "cartesian") or the units
                 * ("degrees" or "m").
                 */
    
                /*
                 * If the axis is not numeric, we can't process any further. If
                 * it is, then adds the coordinate and index ranges.
                 */
                if (axis.isNumeric() && axis instanceof CoordinateAxis1D && !AxisType.Time.equals(type)) {
                    final CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
                    final int length = axis1D.getDimension(0).getLength();
                    if (length > 2 && axis1D.isRegular()) {
                        final double increment = axis1D.getIncrement();
                        final double start = axis1D.getStart();
                        final double end = start + increment * (length - 1); // Inclusive
    
                        if (AxisType.Lon.equals(type) || AxisType.GeoX.equals(type)) {
                            if (increment > 0) {
                                envelope[0] = start;
                                envelope[2] = end;
                            } else {
                                envelope[0] = end;
                                envelope[2] = start;
                            }
                        }
    
                        if (AxisType.Lat.equals(type) || AxisType.GeoY.equals(type)) {
                            if (increment > 0) {
                                envelope[1] = start;
                                envelope[3] = end;
                            } else {
                                envelope[1] = end;
                                envelope[3] = start;
                            }
                        }
                    } else {
    
                        final double[] values = axis1D.getCoordValues();
                        final double val0 = values[0];
                        final double valN = values[values.length - 1];
    
                        if (AxisType.Lon.equals(type) || AxisType.GeoX.equals(type)) {
                            // if (CoordinateAxis.POSITIVE_DOWN
                            // .equalsIgnoreCase(axis.getPositive())) {
                            // envelope[1] = values[0];
                            // envelope[3] = values[values.length - 1];
                            // } else {
                            envelope[0] = val0;
                            envelope[2] = valN;
                            // }
                        }
    
                        if (AxisType.Lat.equals(type) || AxisType.GeoY.equals(type)) {
                            // if (CoordinateAxis.POSITIVE_DOWN
                            // .equalsIgnoreCase(axis.getPositive())) {
                            // envelope[0] = values[0];
                            // envelope[2] = values[values.length - 1];
                            // } else {
                            envelope[1] = val0;
                            envelope[3] = valN;
                            // }
                        }
                    }
                }
            }
            for( int i = 0; i < envelope.length; i++ )
                if (Double.isNaN(envelope[i])) {
                    envelope = null;
                    break;
                }
    
        }
        return envelope;
    }
    
    public static String getCrsType( ucar.nc2.dataset.CoordinateSystem cs ) {
        String crsType;
        // TODO: fix this to handle Vertical instead of Geographic3D
        if (cs.isLatLon()) {
            crsType = cs.hasVerticalAxis() ? UnidataMetadataUtilities.GEOGRAPHIC_3D : UnidataMetadataUtilities.GEOGRAPHIC;
            // csType = UnidataMetadataUtilities.ELLIPSOIDAL;
        } else if (cs.isGeoXY()) {
            crsType = cs.hasVerticalAxis() ? UnidataMetadataUtilities.PROJECTED_3D : UnidataMetadataUtilities.PROJECTED;
            // csType = UnidataMetadataUtilities.CARTESIAN;
        } else {
            throw new RuntimeException("DOCUMENT ME");
        }
        return crsType;
    }

    public static final org.opengis.referencing.crs.CoordinateReferenceSystem WGS84;
    static {
        CoordinateReferenceSystem internalWGS84 = null;
        try {
            internalWGS84 = CRS.decode("EPSG:4326", true);
        } catch (Exception e) {
            internalWGS84 = DefaultGeographicCRS.WGS84;
        }
        WGS84 = internalWGS84;
    }
}
