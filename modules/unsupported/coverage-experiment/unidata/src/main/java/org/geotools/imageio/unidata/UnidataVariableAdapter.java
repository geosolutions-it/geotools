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
import java.awt.image.BandedSampleModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
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
import org.geotools.coverage.io.util.DateRangeComparator;
import org.geotools.coverage.io.util.DateRangeTreeSet;
import org.geotools.coverage.io.util.DoubleRangeTreeSet;
import org.geotools.coverage.io.util.NumberRangeComparator;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.imageio.unidata.cv.CoordinateVariable;
import org.geotools.imageio.unidata.utilities.UnidataCRSUtilities;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableDS;

/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @todo lazy initialization
 * @todo management of data read with proper mangling
 */
public class UnidataVariableAdapter extends CoverageSourceDescriptor {

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

    /**
     * 
     * @author User
     * TODO improve support for this
     */
    public class UnidataAdditionalDomain extends AdditionalDomain {

        /** The detailed domain extent */
        private final Set<Object> domainExtent = new TreeSet<Object>();
        
        /** The merged domain extent */
        private final Set<Object> globalDomainExtent = new TreeSet<Object>();

        /** The domain name */
        private final String name;
        
        private final DomainType type;
        
        final CoordinateVariable<?> adaptee;

        /**
         * @param domainExtent
         * @param globalDomainExtent
         * @param name
         * @param type
         * @param adaptee
         * TODO missing support for Range
         * TODO missing support for String domains
         * @throws IOException 
         */
        public UnidataAdditionalDomain(CoordinateVariable<?> adaptee) throws IOException {
            this.adaptee = adaptee;
            name=adaptee.getName();
            
            // type
            Class<?> type=adaptee.getType();
            if(Date.class.isAssignableFrom(type)){
                this.type=DomainType.DATE;
            } else if(Number.class.isAssignableFrom(type)){
                this.type=DomainType.NUMBER;
            } else {
                throw new UnsupportedOperationException("Unsupported CoordinateVariable:"+adaptee.toString());
            }
            
            // domain
            domainExtent.addAll(adaptee.read());
            
            
            // global domain
            globalDomainExtent.add(new NumberRange<Double>(Double.class,(Double)adaptee.getMinimum(),(Double)adaptee.getMaximum()));
            
            
        }

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
        
    }
    
    final VariableDS variableDS;

    private ucar.nc2.dataset.CoordinateSystem coordinateSystem;

    private UnidataImageReader reader;

    private int numBands;

    private int rank;

    private SampleModel sampleModel;
    
    int numberOfSlices = 0;

    private int width;

    private int height;

    private CoordinateReferenceSystem coordinateReferenceSystem;

    private final static java.util.logging.Logger LOGGER = Logging.getLogger(UnidataVariableAdapter.class);

    /**
     * Extracts the compound {@link CoordinateReferenceSystem} from the unidata variable.
     * 
     * @return the compound {@link CoordinateReferenceSystem}.
     * @throws Exception 
     */
    private void init() throws Exception {
        
        // initialize the various domains
        initSpatialElements();
        
        // initialize rank and number of 2D slices
        initRange();
        
        
        initSlicesInfo();
    }

    /**
     * @throws Exception 
     * 
     */
    private void initSlicesInfo() throws Exception {
        // get the length of the coverageDescriptorsCache in each dimension
        final int[] shape = variableDS.getShape();
        switch (shape.length) {
        case 2:
            numberOfSlices = 1;
            break;
        case 3:
            numberOfSlices = shape[0];
            break;
        case 4:
            numberOfSlices = 0 + shape[0] * shape[1];
            break;
        default:
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.warning("Ignoring variable: " + getName()
                        + " with shape length: " + shape.length);
            
        }

        
    }

    /**
     * @throws IOException 
     * 
     */
    private void initSpatialElements() throws Exception {
        
        List<CoordinateAxis> otherAxes = initCRS();
        
        initSpatialDomain();


        // ADDITIONAL DOMAINS
        if (otherAxes != null&&!otherAxes.isEmpty()) {
            List<AdditionalDomain> additionalDomains = new ArrayList<AdditionalDomain>(otherAxes.size());
            addAdditionalDomain(additionalDomains, otherAxes);
            this.setAdditionalDomains(additionalDomains);
        }
    }

    /**
     * @return
     * @throws IllegalArgumentException
     * @throws RuntimeException
     * @throws IOException
     * @throws IllegalStateException
     */
    private List<CoordinateAxis> initCRS() throws IllegalArgumentException, RuntimeException,
            IOException, IllegalStateException {
        // from UnidataVariableAdapter        
        this.coordinateSystem = UnidataCRSUtilities.getCoordinateSystem(variableDS);
        if (coordinateSystem == null){
            throw new IllegalArgumentException("Provided CoordinateSystem is null");
        }
        // ////
        // Creating the CoordinateReferenceSystem
        // ////
        coordinateReferenceSystem=UnidataCRSUtilities.WGS84;
        
        /*
         * Adds the axis in reverse order, because the NetCDF image reader put the last dimensions in the rendered image. Typical NetCDF convention is
         * to put axis in the (time, depth, latitude, longitude) order, which typically maps to (longitude, latitude, depth, time) order in GeoTools
         * referencing framework.
         */
        final List<CoordinateAxis> axes = coordinateSystem.getCoordinateAxes();
        CoordinateAxis timeAxis = null;
        CoordinateAxis zAxis = null;
        List<CoordinateAxis> otherAxes = new ArrayList<CoordinateAxis>();
        for (CoordinateAxis axis :axes) {
            final AxisType axisType = axis.getAxisType();
            switch(axisType){
            case Time:case RunTime:
                timeAxis = axis;
                continue;
            case GeoZ:case Height:case Pressure:
                String axisName = axis.getFullName();
                if (UnidataCRSUtilities.VERTICAL_AXIS_NAMES.contains(axisName)) {
                    zAxis = axis;
                    continue;
                }else{
                    otherAxes.add(axis);
                }
                continue;  
            case GeoX: case GeoY: case Lat: case Lon:
                // do nothing
                continue;
            default:
                otherAxes.add(axis);
            }
        }

        // END extract necessary info from unidata structures

        // Init temporal domain
        final TemporalCRS temporalCRS = UnidataCRSUtilities.buildTemporalCrs(timeAxis);
        initTemporalDomain(temporalCRS, timeAxis);

        // Init vertical domain
        final VerticalCRS verticalCRS = UnidataCRSUtilities.buildVerticalCrs(coordinateSystem, zAxis);
        initVerticalDomain(verticalCRS, zAxis);
        
        

        return otherAxes;
    }

    /**
     * @param coordinateReferenceSystem
     * @throws MismatchedDimensionException
     * @throws IOException 
     */
    private void initSpatialDomain()
            throws Exception {
        // SPATIAL DOMAIN
        final UnidataSpatialDomain spatialDomain = new UnidataSpatialDomain();
        this.setSpatialDomain(spatialDomain);
        spatialDomain.setCoordinateReferenceSystem(coordinateReferenceSystem);

//        final double[] wsen = UnidataCRSUtilities.getEnvelope(coordinateSystem);
//        final ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(wsen[0], wsen[2],wsen[1], wsen[3], coordinateReferenceSystem);
        spatialDomain.setReferencedEnvelope(reader.boundingBox);
        spatialDomain.setGridGeometry(getGridGeometry());
    }

    /**
     * 
     */
    private void initRange() {
        // set the rank
        rank = variableDS.getRank();
        
        width = variableDS.getDimension(rank - UnidataUtilities.X_DIMENSION).getLength();
        height = variableDS.getDimension(rank - UnidataUtilities.Y_DIMENSION).getLength();
        numBands = rank > 2 ? variableDS.getDimension(2).getLength() : 1;
        
        final int bufferType = UnidataUtilities.getRawDataType(variableDS);
        sampleModel = new BandedSampleModel(bufferType, width, height, 1);
        
        
        // range type
        String description = variableDS.getDescription();
        final StringBuilder sb = new StringBuilder();
        final Set<SampleDimension> sampleDims = new HashSet<SampleDimension>();
        sampleDims.add(new GridSampleDimension(description + ":sd", (Category[]) null, null));

        InternationalString desc = null;
        if (description != null && !description.isEmpty()) {
            desc = new SimpleInternationalString(description);
        }
        final FieldType fieldType = new DefaultFieldType(new NameImpl(getName()), desc, sampleDims);
        sb.append(description != null ? description.toString() + "," : "");
        final RangeType range = new DefaultRangeType(getName(), description, fieldType);
        this.setRangeType(range);        
        
        
    }

    private void addAdditionalDomain(List<AdditionalDomain> additionalDomains, List<CoordinateAxis> otherAxes) {
        for(CoordinateAxis axis:otherAxes){
            
            // look for the wrapper
            final CoordinateVariable<?> cv = reader.coordinatesVariables.get(axis.getFullName());
            if(cv==null){
                LOGGER.info("Unable to map axis "+axis);
                continue;
            }
            
            // create domain
            UnidataAdditionalDomain domain;
            try {
                domain = new UnidataAdditionalDomain(cv);
                additionalDomains.add(domain);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    /**
     * Extracts the {@link GridGeometry2D grid geometry} from the unidata variable.
     * 
     * @return the {@link GridGeometry2D}.
     * @throws IOException 
     */
    protected GridGeometry2D getGridGeometry() throws IOException {
        int[] low = new int[2];
        int[] high = new int[2];
        // String[] axesNames = new String[rank];
        double[] origin = new double[2];
        double scaleX=Double.POSITIVE_INFINITY, scaleY=Double.POSITIVE_INFINITY;

        for( CoordinateVariable<?> cv : reader.coordinatesVariables.values() ) {
            if(!cv.isNumeric()){
                continue;
            }
            final AxisType axisType = cv.getAxisType();
            switch (axisType) {
            case Lon: case GeoX:
                // raster space
                low[0] = 0;
                high[0] = (int) cv.getSize();
                
                // model space
                if(cv.isRegular()){
                    // regular model space
                    origin[0]=cv.getStart();
                    scaleX=cv.getIncrement();
                } else {
                    
                    // model space is not declared to be regular, but we kind of assume it is!!!
                    final int valuesLength=(int) cv.getSize();
                    double min = ((Number)cv.getMinimum()).doubleValue();
                    double max = ((Number)cv.getMaximum()).doubleValue();
                    // make sure we skip nodata coords, bah...
                    if (!Double.isNaN(min) && !Double.isNaN(max)) {
                        origin[0] = min;
                        scaleX = (max-min) / valuesLength;
                    } else {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Axis values contains NaN; finding first valid values");
                        }
                        for( int j = 0; j < valuesLength; j++ ) {
                            double v = ((Number)cv.read(j)).doubleValue();
                            if (!Double.isNaN(v)) {
                                for( int k = valuesLength; k > j; k-- ) {
                                    double vv = ((Number)cv.read(k)).doubleValue();
                                    if (!Double.isNaN(vv)) {
                                        origin[0] = v;
                                        scaleX = (vv - v) / valuesLength;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case Lat: case GeoY:
                // raster space
                low[1] = 0;
                high[1] = (int) cv.getSize();
                
                // model space
                if(cv.isRegular()){
                    scaleY=-cv.getIncrement();
                    origin[1]=cv.getStart()-scaleY*high[1];
                } else {
                    
                    // model space is not declared to be regular, but we kind of assume it is!!!
                    final int valuesLength=(int) cv.getSize();
                    double min = ((Number)cv.getMinimum()).doubleValue();
                    double max = ((Number)cv.getMaximum()).doubleValue();
                    // make sure we skip nodata coords, bah...
                    if (!Double.isNaN(min) && !Double.isNaN(max)) {
                        scaleY = -(max-min) / valuesLength;
                        origin[1] = max;
                    } else {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Axis values contains NaN; finding first valid values");
                        }
                        for( int j = 0; j < valuesLength; j++ ) {
                            double v = ((Number)cv.read(j)).doubleValue();
                            if (!Double.isNaN(v)) {
                                for( int k = valuesLength; k > j; k-- ) {
                                    double vv = ((Number)cv.read(k)).doubleValue();
                                    if (!Double.isNaN(vv)) {
                                        origin[1] = v;
                                        scaleY = -(vv - v) / valuesLength;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            default:
                break;
            }
            
            
        }

        final AffineTransform at = new AffineTransform(scaleX, 0, 0, scaleY, origin[0], origin[1]);
        final GridEnvelope gridRange = new GridEnvelope2D(
                low[0], 
                low[1], 
                high[0]-low[0], 
                high[1]-low[1]);
        final MathTransform raster2Model = ProjectiveTransform.create(at);
        return new GridGeometry2D(gridRange,PixelInCell.CELL_CORNER ,raster2Model, coordinateReferenceSystem,GeoTools.getDefaultHints());
    }

    public int getNumBands() {
        return numBands;
    }

    /**
     * @return the number of dimensions in the variable.
     */
    public int getRank() {
        return rank;
    }

    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Init the Temporal Domain
     * @param wrapper
     * @param temporalCRS
     * @param timeAxis
     * @throws IOException 
     */
    private void initTemporalDomain(final TemporalCRS temporalCRS,
            final CoordinateAxis timeAxis) throws IOException {
        final boolean hasTemporalCRS = temporalCRS != null;
        this.setHasTemporalDomain(hasTemporalCRS);
        if (hasTemporalCRS) {
            final UnidataTemporalDomain temporalDomain = new UnidataTemporalDomain();
            this.setTemporalDomain(temporalDomain);
            temporalDomain.setTemporalCRS(temporalCRS);
            
            // Getting global Extent
            CoordinateVariable<Date> timeDimension = (CoordinateVariable<Date>) reader.coordinatesVariables.get(timeAxis.getShortName());
            Date startTime = timeDimension.getMinimum();
            Date endTime =  timeDimension.getMaximum();
            final DateRange global = new DateRange(startTime, endTime);
            final SortedSet<DateRange> globalTemporalExtent = new DateRangeTreeSet();
            globalTemporalExtent.add(global);
            temporalDomain.globalTemporalExtent = Collections.unmodifiableSortedSet(globalTemporalExtent);
            
            // Getting overall Extent
            final SortedSet<DateRange> extent = new TreeSet<DateRange>(new DateRangeComparator());
            for(Date dd:timeDimension.read()){
                extent.add(new DateRange(dd,dd));
            }
            temporalDomain.setTemporalExtent(extent); 
        }
    }

    /**
     * Init the vertical Domain
     * @param wrapper
     * @param verticalCRS
     * @param zAxis
     * @throws IOException 
     */
    private void initVerticalDomain(final VerticalCRS verticalCRS, final CoordinateAxis zAxis) throws IOException {
        final boolean hasVerticalCRS = verticalCRS != null;
        this.setHasVerticalDomain(hasVerticalCRS);
        if (hasVerticalCRS) {
            final UnidataVerticalDomain verticalDomain = new UnidataVerticalDomain();
            this.setVerticalDomain(verticalDomain);
            verticalDomain.setVerticalCRS(verticalCRS);
            
            // Getting global Extent
            final CoordinateVariable<Double> verticalDimension=(CoordinateVariable<Double>) reader.coordinatesVariables.get(zAxis.getShortName());
            final NumberRange<Double> global = NumberRange.create(
                    verticalDimension.getMinimum(), 
                    verticalDimension.getMaximum());
            final SortedSet<NumberRange<Double>> globalVerticalExtent = new DoubleRangeTreeSet();
            globalVerticalExtent.add(global);
            verticalDomain.globalVerticalExtent = Collections.unmodifiableSortedSet(globalVerticalExtent);
            
            // Getting overall Extent
            final SortedSet<NumberRange<Double>> extent = new TreeSet<NumberRange<Double>>(new NumberRangeComparator());
            for(Double values:verticalDimension.read()){
                extent.add(NumberRange.create(values,values));
            }
            verticalDomain.setVerticalExtent(extent); 
        }
    }

    public UnidataVariableAdapter(UnidataImageReader reader, VariableDS variable) throws Exception {
        this.variableDS = variable;
        this.reader=reader;
        setName(variable.getFullName());
        init();
    }

    @Override
    public UnidataSpatialDomain getSpatialDomain() {
        return (UnidataSpatialDomain) super.getSpatialDomain();
    }

    @Override
    public UnidataTemporalDomain getTemporalDomain() {
        return (UnidataTemporalDomain) super.getTemporalDomain();
    }

    @Override
    public UnidataVerticalDomain getVerticalDomain() {
        return (UnidataVerticalDomain) super.getVerticalDomain();
    }

    /**
     * @return
     */
    int getWidth() {
        return width;
    }

    /**
     * @return
     */
    int getHeight() {
        return height;
    }
}
