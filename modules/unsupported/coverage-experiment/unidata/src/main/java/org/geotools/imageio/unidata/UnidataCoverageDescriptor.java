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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.DefaultDimensionDescriptor;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.io.CoverageSource.AdditionalDomain;
import org.geotools.coverage.io.CoverageSource.DomainType;
import org.geotools.coverage.io.CoverageSource.SpatialDomain;
import org.geotools.coverage.io.CoverageSource.TemporalDomain;
import org.geotools.coverage.io.CoverageSource.VerticalDomain;
import org.geotools.coverage.io.CoverageSourceDescriptor;
import org.geotools.coverage.io.RasterLayout;
import org.geotools.coverage.io.range.FieldType;
import org.geotools.coverage.io.range.RangeType;
import org.geotools.coverage.io.range.impl.DefaultFieldType;
import org.geotools.coverage.io.range.impl.DefaultRangeType;
import org.geotools.coverage.io.util.DateRangeTreeSet;
import org.geotools.coverage.io.util.DoubleRangeTreeSet;
import org.geotools.coverage.io.util.Utilities;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.imageio.Identification;
import org.geotools.imageio.unidata.UnidataUtilities.AxisValueGetter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.VariableDS;

/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @todo lazy initialization
 *
 */
public class UnidataCoverageDescriptor extends CoverageSourceDescriptor {

    public class UnidataSpatialDomain extends SpatialDomain {

        /** The spatial coordinate reference system */
        private CoordinateReferenceSystem coordinateReferenceSystem;

        /** The spatial referenced envelope */
        private ReferencedEnvelope referencedEnvelope;

        /** The gridGeometry of the spatial domain */
        private GridGeometry2D gridGeometry;

        public ReferencedEnvelope getReferencedEnvelope() {
            return referencedEnvelope;
        }

        public void setReferencedEnvelope(ReferencedEnvelope referencedEnvelope) {
            this.referencedEnvelope = referencedEnvelope;
        }

        public GridGeometry2D getGridGeometry() {
            return gridGeometry;
        }

        public double[] getFullResolution() {
            AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
            return CoverageUtilities.getResolution(gridToCRS);
        }

        public void setGridGeometry(GridGeometry2D gridGeometry) {
            this.gridGeometry = gridGeometry;
        }

        public void setCoordinateReferenceSystem(CoordinateReferenceSystem coordinateReferenceSystem) {
            this.coordinateReferenceSystem = coordinateReferenceSystem;
        }

        @Override
        public Set<? extends BoundingBox> getSpatialElements(boolean overall,
                ProgressListener listener) throws IOException {
            return Collections.singleton(referencedEnvelope);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem2D() {
            return coordinateReferenceSystem;
        }

        @Override
        public MathTransform2D getGridToWorldTransform(ProgressListener listener)
                throws IOException {
            return gridGeometry.getGridToCRS2D(PixelOrientation.CENTER);
        }

        @Override
        public Set<? extends RasterLayout> getRasterElements(boolean overall,
                ProgressListener listener) throws IOException {
            Rectangle bounds = gridGeometry.getGridRange2D().getBounds();
            return Collections.singleton(new RasterLayout(bounds));
        }
    }

    public class UnidataTemporalDomain extends TemporalDomain {

        /** The detailed temporal extent */
        private SortedSet<DateRange> temporalExtent;

        /** The merged temporal extent */
        private SortedSet<DateRange> globalTemporalExtent;

        /** The temporal CRS */
        private TemporalCRS temporalCRS;

        public void setTemporalCRS(TemporalCRS temporalCRS) {
            this.temporalCRS = temporalCRS;
        }

        public SortedSet<DateRange> getTemporalExtent() {
            return temporalExtent;
        }

        public void setTemporalExtent(SortedSet<DateRange> temporalExtent) {
            this.temporalExtent = temporalExtent;
        }

        @Override
        public SortedSet<? extends DateRange> getTemporalElements(boolean overall,
                ProgressListener listener) throws IOException {
            if (overall) {
                return globalTemporalExtent;
            } else {
                return temporalExtent;
            }
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return temporalCRS;
        }
    }

    public class UnidataVerticalDomain extends VerticalDomain {

        /** The detailed vertical Extent */
        private SortedSet<NumberRange<Double>> verticalExtent;

        /** The merged vertical Extent */
        private SortedSet<NumberRange<Double>> globalVerticalExtent;

        /** The vertical CRS */
        private VerticalCRS verticalCRS;

        public void setVerticalCRS(VerticalCRS verticalCRS) {
            this.verticalCRS = verticalCRS;
        }

        public void setVerticalExtent(SortedSet<NumberRange<Double>> verticalExtent) {
            this.verticalExtent = verticalExtent;
        }

        public SortedSet<NumberRange<Double>> getVerticalExtent() {
            return verticalExtent;
        }

        @Override
        public SortedSet<? extends NumberRange<Double>> getVerticalElements(boolean overall,
                ProgressListener listener) throws IOException {

            if (overall) {
                return globalVerticalExtent;
            } else {
                return verticalExtent;
            }
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return verticalCRS;
        }
    }

    public class UnidataAdditionalDomain extends AdditionalDomain {

        /** The detailed domain extent */
        private Set<Object> domainExtent;
        
        /** The merged domain extent */
        private Set<Object> globalDomainExtent;

        /** The domain name */
        private String name;
        
        private DomainType type;

        @Override
        public Set<Object> getElements(boolean overall, ProgressListener listener)
                throws IOException {
            if (overall) {
                return globalDomainExtent;
            } else {
                return domainExtent;
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public DomainType getType() {
            return type;
        }

        public Set<Object> getDomainExtent() {
            return domainExtent;
        }

        public void setDomainExtent(Set<Object> domainExtent) {
            this.domainExtent = domainExtent;
        }

        public void setGlobalDomainExtent(Set<Object> globalDomainExtent) {
            this.globalDomainExtent = globalDomainExtent;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setType(DomainType type) {
            this.type = type;
        }
        
    }
    
    private VariableDS variableDS;

    private ucar.nc2.dataset.CoordinateSystem coordinateSystem;

    private UnidataImageReader reader;

    private final static java.util.logging.Logger LOGGER = Logging.getLogger(UnidataCoverageDescriptor.class);

    protected VariableDS getVariableDS() {
        return variableDS;
    }

    protected ucar.nc2.dataset.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Extracts the compound {@link CoordinateReferenceSystem} from the unidata variable.
     * 
     * @return the compound {@link CoordinateReferenceSystem}.
     */
    private void init() {
        final CoordinateSystem cs = UnidataCRSUtilities.getCoordinateSystem(variableDS);
        this.coordinateSystem = cs;

        final List<DimensionDescriptor> dimensions = new ArrayList<DimensionDescriptor>();
        if (cs == null)
            throw new IllegalArgumentException("Provided CoordinateSystem is null");

        String crsName = "Unknown";
        // String csName = cs.getName(); TODO check
        String csName = "Unknown";
        String datumName = "Unknown";

        String crsType = UnidataCRSUtilities.getCrsType(cs);

        double greenwichLon = 0.0;
        String primeMeridianName = null;

        double semiMajorAxis = 6378137.0;
        // double semiMinorAxis;
        double invFlattening = 298.257223563;
        String secondDefiningParameter = null;
        // String unit = "meter";
        String ellipsoidName = null;

        if (crsType == UnidataMetadataUtilities.GEOGRAPHIC || crsType == UnidataMetadataUtilities.GEOGRAPHIC_3D) {
            Identification crsIdentification = new Identification("WGS 84", null, null, "EPSG:4326");
            crsName = crsIdentification.getName();
            csName = new Identification("WGS 84", null, null, null).getName();
            datumName = new Identification("WGS_1984", "World Geodetic System 1984", null,
                    "EPSG:6326").getName();

            primeMeridianName = new Identification("Greenwich", null, null, "EPSG:8901").getName();
            ellipsoidName = new Identification("WGS 84", null, null, "EPSG:7030").getName();

            /*
             * XXX following is how the second def parameter is set.
             * 
             * right now the semiMinorAxis is not defined, therefore we hardcode the thing
             */
            secondDefiningParameter = UnidataMetadataUtilities.MD_DTM_GD_EL_INVERSEFLATTENING;

        } else if (crsType == UnidataMetadataUtilities.PROJECTED
                || crsType == UnidataMetadataUtilities.PROJECTED_3D) {
            // TODO Handle this case ... we need an example of netCDF projected
            // CoordinateReferenceSystem
            throw new RuntimeException("Projected/Projected_3D CRS is not implemented yet");
        }
        /*
         * Adds the axis in reverse order, because the NetCDF image reader put the last dimensions in the rendered image. Typical NetCDF convention is
         * to put axis in the (time, depth, latitude, longitude) order, which typically maps to (longitude, latitude, depth, time) order in GeoTools
         * referencing framework.
         */
        final List<CoordinateAxis> axes = cs.getCoordinateAxes();
        CoordinateAxis timeAxis = null;
        CoordinateAxis zAxis = null;
        List<CoordinateAxis> otherAxes = new ArrayList<CoordinateAxis>();
        // TODO check this???
        // for( int i = axes.size(); --i >= cs.getRankDomain() - 2; ) {
        for (int i = 0; i < axes.size(); i++) {
            CoordinateAxis axis = axes.get(i);
            final AxisType axisType = axis.getAxisType();
            if (AxisType.Time.equals(axisType)) {
                timeAxis = axis;
                continue;
            }

            /*
             * If the axis is not numeric, we can't process any further. If it is, then adds the coordinate and index ranges.
             */
            if (!axis.isNumeric()) {
                continue;
            }
            if (axisType == AxisType.Height || axisType == AxisType.GeoZ
                    || axisType == AxisType.Pressure) {
                
                //TODO: check that.
                String axisName = axis.getFullName();
                if (UnidataCRSUtilities.VERTICAL_AXIS_NAMES.contains(axisName)) {
                    zAxis = axis;
                    continue;
                }
            }

            if (axisType != AxisType.Lat && axisType != AxisType.Lon) {
                otherAxes.add(axis);
            }
        }

        // END extract necessary info from unidata structures

        final CSFactory csFactory = UnidataCRSUtilities.FACTORY_CONTAINER.getCSFactory();
        final DatumFactory datumFactory = UnidataCRSUtilities.FACTORY_CONTAINER.getDatumFactory();
        final CRSFactory crsFactory = UnidataCRSUtilities.FACTORY_CONTAINER.getCRSFactory();

        // Init temporal domain
        final TemporalCRS temporalCRS = UnidataCRSUtilities.buildTemporalCrs(csName, crsName, timeAxis, csFactory, datumFactory, crsFactory);
        initTemporalDomain(temporalCRS, timeAxis, dimensions);

        // Init vertical domain
        final VerticalCRS verticalCRS = UnidataCRSUtilities.buildVerticalCrs(cs, csName, zAxis, csFactory, datumFactory, crsFactory);
        initVerticalDomain(verticalCRS, zAxis, dimensions);
        
        

        // ////
        // Creating the CoordinateReferenceSystem
        // ////
        String name = crsName;
        if (name == null) {
            name = "Unknown";
        }
        boolean isGeographic = false; // TODO is this really necessary?
        CoordinateReferenceSystem coordinateReferenceSystem = null;
        if (name.contains("WGS84") || name.contains("WGS 84")) {
            coordinateReferenceSystem = (name.contains("3D")) ? DefaultGeographicCRS.WGS84_3D
                    : UnidataCRSUtilities.WGS84;
            isGeographic = true;
        }

        if (coordinateReferenceSystem == null) {
            String type = crsType;
            if (type == null) {
                type = isGeographic ? UnidataMetadataUtilities.GEOGRAPHIC : UnidataMetadataUtilities.PROJECTED;
            }
            final Map<String, String> map = Collections.singletonMap("name", name);
            try {
                Datum datum = UnidataCRSUtilities.getDatum(datumName, greenwichLon,
                        primeMeridianName, ellipsoidName, semiMajorAxis, invFlattening,
                        ellipsoidName, secondDefiningParameter);
                if (type.equalsIgnoreCase(UnidataMetadataUtilities.GEOGRAPHIC) || type.equalsIgnoreCase(UnidataMetadataUtilities.GEOGRAPHIC_3D)) {

                    coordinateReferenceSystem = crsFactory.createGeographicCRS(map, (GeodeticDatum) datum,
                            (EllipsoidalCS) UnidataCRSUtilities.getCoordinateSystem(csName, axes));
                } else {
                    // TODO check implementation - non geographic is not supported yet
                    throw new RuntimeException("Only Geographic CRS is currently supported.");

                    // final Map<String, String> baseMap = Collections.singletonMap("name",
                    // crsName);// metaCRS.getBaseCRS().getName());
                    // final GeographicCRS baseCRS = crsFactory.createGeographicCRS(baseMap,
                    // (GeodeticDatum) datum,
                    // DefaultEllipsoidalCS.GEODETIC_2D);
                    // crs = crsFactory.createProjectedCRS(map, baseCRS, getProjection(metaCRS),
                    // (CartesianCS) getCoordinateSystem(metaCRS));
                }
            } catch (Throwable e) {
                coordinateReferenceSystem = null;
            }
        }
        final UnidataSpatialDomain spatialDomain = new UnidataSpatialDomain();
        this.setSpatialDomain(spatialDomain);
        spatialDomain.setCoordinateReferenceSystem(coordinateReferenceSystem);

        final double[] wsen = UnidataCRSUtilities.getEnvelope(cs);
        final ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(wsen[0], wsen[2],
                wsen[1], wsen[3], coordinateReferenceSystem);
        spatialDomain.setReferencedEnvelope(referencedEnvelope);
        final GridGeometry2D gridGeometry = getGridGeometry(variableDS, coordinateReferenceSystem);
        spatialDomain.setGridGeometry(gridGeometry);

        String description = variableDS.getDescription();
        final StringBuilder sb = new StringBuilder();
        final Set<SampleDimension> sampleDims = new HashSet<SampleDimension>();
        sampleDims.add(new GridSampleDimension(description + ":sd", (Category[]) null, null));

        InternationalString desc = null;
        if (description != null && !description.isEmpty()) {
            desc = new SimpleInternationalString(description);
        }
        final FieldType fieldType = new DefaultFieldType(new NameImpl(name), desc, sampleDims);
        sb.append(description != null ? description.toString() + "," : "");
        final RangeType range = new DefaultRangeType(name, description, fieldType);
        this.setRangeType(range);
        
        if (otherAxes != null) {
            List<AdditionalDomain> additionalDomains = new ArrayList<AdditionalDomain>(otherAxes.size());
            this.setAdditionalDomains(additionalDomains);
            for (CoordinateAxis axis: otherAxes) {
               addAdditionalDomain(additionalDomains, axis, dimensions);
            }
        }
        setDimensionDescriptors(dimensions);
    }

    private void addAdditionalDomain(List<AdditionalDomain> additionalDomains, CoordinateAxis axis, List<DimensionDescriptor> dimensions) {
        UnidataAdditionalDomain domain = new UnidataAdditionalDomain();
        domain.setName(axis.getShortName());
        
        // TODO: Consider handling more types:
        AxisValueGetter builder = new AxisValueGetter(axis);
        final int numValues = builder.getNumValues();
        Set<Object> extent = new TreeSet<Object>();
        Set<Object> globalExtent = new HashSet<Object>();
        final Double[] firstLast = new Double[2];
        for (int i = 0; i < numValues; i++) {
            Double value = builder.build(i);
            if (i == 0 ) {
                firstLast[0] = value;
            } else if (i == numValues - 1 ) {
                firstLast[1] = value;
            }
            extent.add(value);
        }
        domain.setDomainExtent(extent);
        if (firstLast[0] > firstLast[1]) {
            double t = firstLast[0];
            firstLast[0] = firstLast[1];
            firstLast[1] = t;
        }
        globalExtent.add(new NumberRange<Double>(Double.class, firstLast[0], firstLast[1]));
        domain.setGlobalDomainExtent(globalExtent);
        
        domain.setType(DomainType.NUMBER);
        additionalDomains.add(domain);
        // TODO: Parse Units from axis and map them to UCUM units
        dimensions.add(new DefaultDimensionDescriptor(axis.getShortName(), "FIXME_UNIT", "FIXME_UNITSYMBOL", axis.getShortName(), null));
    }

    /**
     * Init the Temporal Domain
     * @param wrapper
     * @param temporalCRS
     * @param timeAxis
     * @param dimensions 
     */
    private void initTemporalDomain(final TemporalCRS temporalCRS, final CoordinateAxis timeAxis, final List<DimensionDescriptor> dimensions) {
        final boolean hasTemporalCRS = temporalCRS != null;
        this.setHasTemporalDomain(hasTemporalCRS);
        if (hasTemporalCRS) {
            final UnidataTemporalDomain temporalDomain = new UnidataTemporalDomain();
            this.setTemporalDomain(temporalDomain);
            temporalDomain.setTemporalCRS(temporalCRS);

            // Getting global Extent
            final DateRange global = UnidataUtilities.getTemporalExtent(timeAxis);
            final SortedSet<DateRange> globalTemporalExtent = new DateRangeTreeSet();
            globalTemporalExtent.add(global);
            temporalDomain.globalTemporalExtent = Collections.unmodifiableSortedSet(globalTemporalExtent);

            // Getting overall Extent
            final SortedSet<DateRange> extent = UnidataUtilities.getTemporalExtentSet(timeAxis);
            extent.add(global);
            temporalDomain.setTemporalExtent(extent); 
            //TODO: Fix that once schema attributes to dimension mapping is merged from Simone's code
            dimensions.add(new DefaultDimensionDescriptor(Utils.TIME_DOMAIN, 
                    DefaultDimensionDescriptor.UCUM.TIME_UNITS.getName(), DefaultDimensionDescriptor.UCUM.TIME_UNITS.getSymbol(), Utils.TIME_DOMAIN.toLowerCase(), null));
        }
    }

    /**
     * Init the vertical Domain
     * @param wrapper
     * @param verticalCRS
     * @param zAxis
     * @param dimensions 
     */
    private void initVerticalDomain(final VerticalCRS verticalCRS, final CoordinateAxis zAxis, final List<DimensionDescriptor> dimensions) {
        final boolean hasVerticalCRS = verticalCRS != null;
        this.setHasVerticalDomain(hasVerticalCRS);
        if (hasVerticalCRS) {
            final UnidataVerticalDomain verticalDomain = new UnidataVerticalDomain();
            this.setVerticalDomain(verticalDomain);
            verticalDomain.setVerticalCRS(verticalCRS);

            // Getting global Extent
            final NumberRange<Double> global = UnidataUtilities.getVerticalExtent(zAxis);
            final SortedSet<NumberRange<Double>> globalVerticalExtent = new DoubleRangeTreeSet();
            globalVerticalExtent.add(global);
            verticalDomain.globalVerticalExtent = Collections.unmodifiableSortedSet(globalVerticalExtent);

            // Getting overall Extent
            final SortedSet<NumberRange<Double>> extent = UnidataUtilities.getVerticalExtentSet(zAxis);
            extent.add(global);
            verticalDomain.setVerticalExtent(extent);
            //TODO: Map ZAxis unit to UCUM UNIT (depending on type... elevation, level, pressure, ...)
            //TODO: Fix that once schema attributes to dimension mapping is merged from Simone's code 
            dimensions.add(new DefaultDimensionDescriptor(Utils.ELEVATION_DOMAIN, 
                    DefaultDimensionDescriptor.UCUM.TIME_UNITS.getName(), DefaultDimensionDescriptor.UCUM.ELEVATION_UNITS.getSymbol(), Utils.ELEVATION_DOMAIN.toLowerCase(), null));
        }
    }

    /**
     * Extracts the {@link GridGeometry2D grid geometry} from the unidata variable.
     * 
     * @return the {@link GridGeometry2D}.
     */
    protected GridGeometry2D getGridGeometry(VariableDS variable, CoordinateReferenceSystem crs) {
        GridGeometry2D gridGeometry = null;
        final List<ucar.nc2.dataset.CoordinateSystem> systems = variable.getCoordinateSystems();
        if (!systems.isEmpty()) {
            int rank = variable.getRank() - (systems.get(0).hasTimeAxis() ? 1 : 0);
    
            List<Dimension> dimensions = variable.getDimensions();
            int i = rank - 1;
            int[] low = new int[rank];
            int[] high = new int[rank];
            // String[] axesNames = new String[rank];
            double[] origin = new double[rank];
            double[][] offsetVectors = new double[rank][rank];
    
            for( Dimension dim : dimensions ) {
                final Variable axisVar = reader.getCoordinate(dim.getName());
                if (axisVar != null && axisVar instanceof CoordinateAxis) {
                    final CoordinateAxis coordAxis = (CoordinateAxis) axisVar;
                    final AxisType axisType = coordAxis.getAxisType();
                    if (!AxisType.Time.equals(axisType)) {
                        if (!AxisType.GeoZ.equals(axisType) && !AxisType.Height.equals(axisType)
                                && !AxisType.Pressure.equals(axisType)) {
                            low[i] = 0;
                            high[i] = dim.getLength();
                        } 
                        if (i < 4 && reader.getCoordinate(dim.getName()) != null) {
                            if (coordAxis.isNumeric() && coordAxis instanceof CoordinateAxis1D) {
                                final CoordinateAxis1D axis1D = (CoordinateAxis1D) coordAxis;
                                final int length = axis1D.getDimension(0).getLength();
                                if (length > 2 && axis1D.isRegular()) {
                                    // Reminder: pixel orientation is
                                    // "center",
                                    // maximum value is inclusive.
                                    final double increment = axis1D.getIncrement();
                                    final double start = axis1D.getStart();
                                    final double end = start + increment * (length - 1); // Inclusive
                                    if (axisType.equals(AxisType.Lat)) { //Check this
                                        origin[i] = end;
                                        offsetVectors[i][i] = (start - end) / length;
                                    } else {
                                        origin[i] = start;
                                        offsetVectors[i][i] = (end - start) / length;
                                    }
                                    i--;
                                } else {
                                    final double[] values = axis1D.getCoordValues();
                                    if (values != null) {
                                        final int valuesLength = values.length;
                                        if (valuesLength >= 2) {
                                            if (!Double.isNaN(values[0]) && !Double.isNaN(values[values.length - 1])) {
                                                origin[i] = values[0];
                                                offsetVectors[i][i] = (values[values.length - 1] - values[0]) / length;
                                                i--;
                                            } else {
                                                if (LOGGER.isLoggable(Level.FINE)) {
                                                    LOGGER.log(Level.FINE, "Axis values contains NaN; finding first valid values");
                                                }
                                                for( int j = 0; j < valuesLength; j++ ) {
                                                    double v = values[j];
                                                    if (!Double.isNaN(v)) {
                                                        for( int k = valuesLength; k > j; k-- ) {
                                                            double vv = values[k];
                                                            if (!Double.isNaN(vv)) {
                                                                origin[i] = v;
                                                                offsetVectors[i][i] = (vv - v) / length;
                                                                i--;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            origin[i] = values[0];
                                            offsetVectors[i][i] = 0;
                                            i--;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    
            final AffineTransform at = Utilities.getAffineTransform(origin, offsetVectors);
            final GridEnvelope gridRange = Utilities.getGridRange(high, low);
            final MathTransform raster2Model = ProjectiveTransform.create(at);
    
            gridGeometry = new GridGeometry2D(gridRange, raster2Model, crs);
        }
        return gridGeometry;
    }

    public UnidataCoverageDescriptor(UnidataImageReader reader, VariableDS variable) {
        this.variableDS = variable;
        this.reader=reader;
        setName(variable.getFullName());
        init();
    }
    
}
