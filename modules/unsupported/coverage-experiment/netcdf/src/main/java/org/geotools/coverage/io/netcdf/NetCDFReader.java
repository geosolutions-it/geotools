/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2013, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io.netcdf;

import it.geosolutions.imageio.utilities.SoftValueHashMap;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.io.CoverageAccess.AccessType;
import org.geotools.coverage.io.CoverageReadRequest;
import org.geotools.coverage.io.CoverageResponse;
import org.geotools.coverage.io.CoverageSource;
import org.geotools.coverage.io.CoverageSource.TemporalDomain;
import org.geotools.coverage.io.CoverageSource.VerticalDomain;
import org.geotools.coverage.io.Driver.DriverCapabilities;
import org.geotools.coverage.io.GridCoverageResponse;
import org.geotools.coverage.io.util.DateRangeComparator;
import org.geotools.coverage.io.util.NumberRangeComparator;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.imageio.unidata.UnidataCoverageDescriptor;
import org.geotools.imageio.unidata.UnidataCoverageDescriptor.UnidataSpatialDomain;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.type.Name;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

/**
 * A NetCDF Reader implementation
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class NetCDFReader extends AbstractGridCoverage2DReader {

    private final static Logger LOGGER = Logging
            .getLogger("org.geotools.coverage.io.netcdf.NetCDFReader");

    static NetCDFDriver DRIVER = new NetCDFDriver();
    
    static NumberRangeComparator COMPARATOR = new NumberRangeComparator();

    private NetCDFAccess access = null;

    private List<Name> names = null;

    private Set<String> setNames = null;

    private URL sourceURL;

    private SoftValueHashMap<String, CoverageSource> coverages = new SoftValueHashMap<String, CoverageSource>();

    public NetCDFReader(Object input, Hints uHints) throws DataSourceException{
        super(input, uHints);
        sourceURL = checkSource(input);

        if (!DRIVER.canProcess(DriverCapabilities.CONNECT, sourceURL, null)) {
            throw new DataSourceException("unable to connect to the specified source " + sourceURL);
        }

        // getting access to the source
        try {
            access = (NetCDFAccess) DRIVER.process(DriverCapabilities.CONNECT, sourceURL, null, null, null);
        } catch (IOException e) {
            throw new DataSourceException("Unable to connect", e);
        }
        if (access == null) {
            throw new DataSourceException("Unable to connect");
        }

        LOGGER.info("ACCEPTED: " + source.toString());

        // get the names
        names = access.getNames(null);
        setNames = new HashSet<String>();
        for (Name name: names) {
            setNames.add(name.toString());
        }
    }

    private URL checkSource(Object input) {
        URL sourceURL = null;
        if (input instanceof URL) {
            sourceURL = (URL) input;
        } else if (input instanceof File) {
            sourceURL = DataUtilities.fileToURL((File) input);
        }
        return sourceURL;
    }

    @Override
    public Format getFormat() {
        return new NetCDFFormat();
    }

    @Override
    public String[] getMetadataNames(String coverageName) {
        checkIsSupported(coverageName);
        final List<String> metadataNames = new ArrayList<String>();
        metadataNames.add(AbstractGridCoverage2DReader.TIME_DOMAIN);
        metadataNames.add(AbstractGridCoverage2DReader.HAS_TIME_DOMAIN);
        metadataNames.add(AbstractGridCoverage2DReader.TIME_DOMAIN_MINIMUM);
        metadataNames.add(AbstractGridCoverage2DReader.TIME_DOMAIN_MAXIMUM);
        metadataNames.add(AbstractGridCoverage2DReader.TIME_DOMAIN_RESOLUTION);
        metadataNames.add(AbstractGridCoverage2DReader.ELEVATION_DOMAIN);
        metadataNames.add(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MINIMUM);
        metadataNames.add(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MAXIMUM);
        metadataNames.add(AbstractGridCoverage2DReader.HAS_ELEVATION_DOMAIN);
        metadataNames.add(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_RESOLUTION);

        // TODO: Check for custom domains
        return metadataNames.toArray(new String[metadataNames.size()]);
    }

    @Override
    public String getMetadataValue(String coverageName, String name) {
        String value = null;
        NetCDFSource source = null;
        TemporalDomain timeDomain;
        try {
            source = (NetCDFSource) getGridCoverageSource(coverageName);
            timeDomain = source.getTemporalDomain();

            final VerticalDomain verticalDomain = source.getVerticalDomain();
            final boolean hasTimeDomain = timeDomain != null;
            final boolean hasElevationDomain = verticalDomain != null;

            if (name.equalsIgnoreCase(AbstractGridCoverage2DReader.HAS_ELEVATION_DOMAIN))
                return String.valueOf(hasElevationDomain);

            if (name.equalsIgnoreCase(AbstractGridCoverage2DReader.HAS_TIME_DOMAIN)) {
                return String.valueOf(hasTimeDomain);
            }

            // NOT supported
            if (name.equalsIgnoreCase(AbstractGridCoverage2DReader.TIME_DOMAIN_RESOLUTION)) {
                return null;
            }
            // NOT supported
            if (name.equalsIgnoreCase(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_RESOLUTION)) {
                return null;
            }

            if (hasTimeDomain) {
                if (name.equalsIgnoreCase("time_domain")) {
                    return parseDomain(name, timeDomain);
                }
                if ((name.equalsIgnoreCase("time_domain_minimum") || name
                        .equalsIgnoreCase("time_domain_maximum"))) {
                    return parseDomain(name, timeDomain);
                }
            }

            if (hasElevationDomain) {
                if (name.equalsIgnoreCase("elevation_domain")) {
                    return parseDomain(name, verticalDomain);
                }

                if (name.equalsIgnoreCase("elevation_domain_minimum")
                        || name.equalsIgnoreCase("elevation_domain_maximum")) {
                    return parseDomain(name, verticalDomain);
                }
            }

            // TODO check additional domains
            // if (manager.domainsManager != null) {
            // return manager.domainsManager.getMetadataValue(name);
            // }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        return value;
    }

    @Override
    public String[] getGridCoverageNames() {
        return (String[]) setNames.toArray(new String[setNames.size()]);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        return Collections.emptySet();
    }

    @Override
    public int getGridCoverageCount() {
        return setNames.size();
    }

    /**
     * Parse a domain 
     * @param name
     * @param domain
     * @return
     * @throws IOException
     */
    private String parseDomain(String name, Object domain) throws IOException {
        name = name.toLowerCase();
        if (domain instanceof VerticalDomain) {
            
            // Vertical domain management
            VerticalDomain verticalDomain = (VerticalDomain) domain;
            if (name.endsWith("domain")) {
                
                // global domain
                SortedSet<? extends NumberRange<Double>> verticalElements = verticalDomain
                        .getVerticalElements(false, null);
                return buildVerticalList(verticalElements);
            } else {
                // min or max requests
                SortedSet<? extends NumberRange<Double>> verticalElements = verticalDomain
                        .getVerticalElements(true, null);
                NumberRange<Double> overall = verticalElements.iterator().next();
                if (name.endsWith("maximum")) {
                    return Double.toString(overall.getMaximum());
                } else if (name.endsWith("minimum")) {
                    return Double.toString(overall.getMinimum());
                } else {
                    throw new IllegalArgumentException("Unsupported metadata name");
                }
            }
        } else if (domain instanceof TemporalDomain) {
            
            // Temporal domain management
            TemporalDomain temporalDomain = (TemporalDomain) domain;
            if (name.endsWith("domain")) {
                // global domain
                SortedSet<? extends DateRange> temporalElements = temporalDomain.getTemporalElements(false, null);
                return buildTemporalList(temporalElements);
            } else {
                SortedSet<? extends DateRange> temporalElements = temporalDomain.getTemporalElements(true, null);
                DateRange overall = temporalElements.iterator().next();
                // min or max requests
                if (name.endsWith("maximum")) {
                    return ConvertersHack.convert(overall.getMaxValue(), String.class);
                } else if (name.endsWith("minimum")) {
                    return ConvertersHack.convert(overall.getMinValue(), String.class);
                } else {
                    throw new IllegalArgumentException("Unsupported metadata name");
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported domain ");
        }
    }

    private String buildTemporalList(SortedSet<? extends DateRange> temporalElements) {
        Iterator<DateRange> iterator = (Iterator<DateRange>) temporalElements.iterator();
//        LinkedHashSet<String> result = new LinkedHashSet<String>();

        if (iterator.hasNext()) {
          //Skipping the overall range introduced by the new APIs
            iterator.next();
        }
        final StringBuilder buff = new StringBuilder("");
        while (iterator.hasNext()) {
            DateRange range = iterator.next();
            buff.append(ConvertersHack.convert(range.getMinValue(), String.class) + "/" + ConvertersHack.convert(range.getMaxValue(), String.class));
            if (iterator.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

    /**
     * Setup a String containing vertical domain by doing a scan of a set of vertical Elements
     * @param verticalElements
     * @return
     */
    private String buildVerticalList(SortedSet<? extends NumberRange<Double>> verticalElements) {
        Iterator<NumberRange<Double>> iterator = (Iterator<NumberRange<Double>>) verticalElements
                .iterator();
        LinkedHashSet<String> ranges = new LinkedHashSet<String>();

        if (iterator.hasNext()) {
          //Skipping the overall range introduced by the new APIs
            iterator.next();
        }
        while (iterator.hasNext()) {
            NumberRange<Double> range = iterator.next();
            ranges.add((range.getMinValue() + "/" + range.getMaxValue()));
        }
        return buildResultsString(ranges);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        final CoverageSource gridSource = getGridCoverageSource(coverageName);
        final CoverageReadRequest request = setupCoverageRequest(parameters);
        final CoverageResponse result = gridSource.read(request, null);
        Collection<? extends Coverage> results = result.getResults(null);
        if (results == null || results.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("No results have been found");
            }
            return null;
        }
        GridCoverageResponse resp = (GridCoverageResponse) results.iterator().next();
        return resp.getGridCoverage2D();
    }

    /**
     * Create the coverageReadRequest on top of the specified read params 
     * @param params
     * @return
     * @throws IOException
     */
    private CoverageReadRequest setupCoverageRequest(GeneralParameterValue[] params) throws IOException {
        CoverageReadRequest request = new CoverageReadRequest();
        if (params != null) {
            for (GeneralParameterValue gParam : params) {
                if (gParam instanceof ParameterValue<?>) {
                    final ParameterValue<?> param = (ParameterValue<?>) gParam;
                    final ReferenceIdentifier name = param.getDescriptor().getName();
                    try {
                        extractParameter(param, name, request);
                    } catch (MismatchedDimensionException e) {
                        throw new IOException(e);
                    } catch (InvalidGridGeometryException e) {
                        throw new IOException(e);
                    } catch (TransformException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        return request;
    }

    private void extractParameter(ParameterValue<?> param, ReferenceIdentifier name,
            CoverageReadRequest request) throws MismatchedDimensionException,
            InvalidGridGeometryException, TransformException {

        // //
        //
        // Requested GridGeometry2D parameter
        //
        // //
        if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName())) {
            final Object value = param.getValue();
            if (value == null)
                return;
            final GridGeometry2D gg = (GridGeometry2D) value;
            request.setDomainSubset(gg.getGridRange2D(), gg.getGridToCRS2D(),
                    gg.getCoordinateReferenceSystem());
            return;
        }

        // // //
        // //
        // // Merge Behavior
        // //
        // // //
        // if (name.equals(ImageMosaicFormat.MERGE_BEHAVIOR.getName())) {
        // final Object value = param.getValue();
        // if(value==null)
        // return;
        // mergeBehavior = MergeBehavior.valueOf(param.stringValue().toUpperCase());
        // return;
        // }

        // // //
        // //
        // // Interpolation parameter
        // //
        // // //
        // if (name.equals(ImageMosaicFormat.INTERPOLATION.getName())) {
        // final Object value = param.getValue();
        // if(value==null)
        // return;
        // interpolation = (Interpolation) value;
        // return;
        // }
        //
        //
        // if (name.equals(
        // ImageMosaicFormat.BACKGROUND_VALUES.getName())) {
        // final Object value = param.getValue();
        // if(value==null)
        // return;
        // backgroundValues = (double[]) value;
        // return;
        //
        // }
        //
        //
        // if (name.equals(ImageMosaicFormat.ALLOW_MULTITHREADING.getName())) {
        // final Object value = param.getValue();
        // if(value==null)
        // return;
        // multithreadingAllowed = ((Boolean) value).booleanValue();
        // return;
        // }
        //
        // //
        //
        // Time parameter
        //
        // //
        if (name.equals(ImageMosaicFormat.TIME.getName())) {
            final Object value = param.getValue();
            if (value == null) {
                return;
            }
            final List<?> dates = (List<?>) value;
            if (dates != null && !dates.isEmpty()) {
                SortedSet<DateRange> requestedTemporalSubset = new TreeSet<DateRange>(new DateRangeComparator());
                for (Object val : dates) {
                    if (val instanceof Date) {
                        requestedTemporalSubset.add(new DateRange((Date) val, (Date) val));
                    } else if (val instanceof DateRange) {
                        requestedTemporalSubset.add((DateRange)val);
                    }
                }
                
                // TODO IMPROVE THAT TO DEAL ON RANGES
                request.setTemporalSubset(requestedTemporalSubset);
            }
            return;

        }

        //
        //
        // Elevation parameter
        //
        // //
        if (name.equals(ImageMosaicFormat.ELEVATION.getName())) {
            final Object value = param.getValue();
            if (value == null)
                return;
            List<?> values = (List<?>) value;
            if (values != null && !values.isEmpty()) {
                Set<NumberRange<Double>> verticalSubset = new TreeSet<NumberRange<Double>>(COMPARATOR);
                for (Object val : values) {
                    if (val instanceof Number) {
                        verticalSubset.add(new NumberRange<Double>(Double.class, ((Number) val).doubleValue(), ((Number) val).doubleValue()));
                    } else if (val instanceof NumberRange) {
                        verticalSubset.add((NumberRange<Double>)val);
                    }
                }
                // TODO IMPROVE THAT TO DEAL ON RANGES
                request.setVerticalSubset(verticalSubset);
            }
            return;
        }
    }

    /**
     * Return a {@link CoverageSource} related to the specified coverageName
     * @param coverageName
     * @return
     * @throws IOException
     */
    private CoverageSource getGridCoverageSource(final String coverageName) throws IOException {
        // Preliminar check on name availability
        checkIsSupported(coverageName);
        synchronized (coverages) {
            if (coverages.containsKey(coverageName)) {
                return coverages.get(coverageName);
            }

            // create, cache and return
            CoverageSource source = access.access(new NameImpl(coverageName), null,
                    AccessType.READ_ONLY, null, null);
            coverages.put(coverageName, source);
            return source;
        }
    }

    /**
     * Check whether the specified coverageName is one of the coverage available for the reader
     * @param coverageName
     */
    private void checkIsSupported(final String coverageName) {
        if (!setNames.contains(coverageName)) {
            throw new IllegalArgumentException("the specified coverage is not available: "
                    + coverageName);
        }
    }

    /**
     * Read a GridCoverage2D base on the specified read parameters.
     */
    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException,
            IOException {
        if (!names.isEmpty()) {
            if (names.size() > 1) {
                throw new IllegalArgumentException("You need to specify a coverageName");
            } else {
                return read(names.get(0).toString(), parameters);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (coverages) {
            if (coverages != null && !coverages.isEmpty()) {
                Iterator<String> keysIt = coverages.keySet().iterator();
                while (keysIt.hasNext()) {
                    String key = keysIt.next();
                    CoverageSource sourceCov = coverages.get(key);
                    sourceCov.dispose();
                }
            }
            coverages.clear();
            coverages = null;
        }
        if (access != null) {
            try {
                access.dispose();
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, t.getLocalizedMessage(), t);
                }
            }
        }
    }

    /**
     * Build a String containing comma separated values from the result set
     * @param result
     * @return
     */
    private String buildResultsString(Set<String> result) {
        if (result.size() <= 0) {
            return "";
        }

        final StringBuilder buff = new StringBuilder();
        for (Iterator<String> it = result.iterator(); it.hasNext();) {
            buff.append(ConvertersHack.convert(it.next(), String.class));
            if (it.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope(final String coverageName) {
        try {
            CoverageSource source = getGridCoverageSource(coverageName);
            UnidataCoverageDescriptor.UnidataSpatialDomain spatialDomain = (UnidataSpatialDomain) source.getSpatialDomain();
            GeneralEnvelope generalEnvelope = new GeneralEnvelope(spatialDomain.getReferencedEnvelope());
            generalEnvelope.setCoordinateReferenceSystem(spatialDomain.getCoordinateReferenceSystem2D());
            return generalEnvelope;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GridEnvelope getOriginalGridRange(final String coverageName) {
        try {
            final CoverageSource source = getGridCoverageSource(coverageName);
            UnidataCoverageDescriptor.UnidataSpatialDomain spatialDomain = (UnidataSpatialDomain) source
                    .getSpatialDomain();
            return spatialDomain.getGridGeometry().getGridRange2D();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double[] getReadingResolutions(String coverageName, OverviewPolicy policy,
            double[] requestedResolution) throws IOException {
            // Currently we have no overviews support so we will return the highest resolution
        final CoverageSource source = getGridCoverageSource(coverageName);
            UnidataCoverageDescriptor.UnidataSpatialDomain spatialDomain = (UnidataSpatialDomain) source
                    .getSpatialDomain();
            GeneralGridGeometry gridGeometry2D = spatialDomain.getGridGeometry();
            AffineTransform gridToCRS = (AffineTransform) gridGeometry2D.getGridToCRS();
            return CoverageUtilities.getResolution(gridToCRS);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(final String coverageName) {
        try {
            final CoverageSource source = getGridCoverageSource(coverageName);
            UnidataCoverageDescriptor.UnidataSpatialDomain spatialDomain = (UnidataSpatialDomain) source
                    .getSpatialDomain();
            return spatialDomain.getCoordinateReferenceSystem2D();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        try {
            final CoverageSource source = getGridCoverageSource(coverageName);
            UnidataCoverageDescriptor.UnidataSpatialDomain spatialDomain = (UnidataSpatialDomain) source.getSpatialDomain();
            MathTransform2D gridToWorld = spatialDomain.getGridToWorldTransform(null);
            if (pixInCell == PixelInCell.CELL_CENTER) {
                return gridToWorld;
            }

            // we do have to change the pixel datum
            if (gridToWorld instanceof AffineTransform) {
                final AffineTransform tr = new AffineTransform((AffineTransform) gridToWorld);
                tr.concatenate(AffineTransform.getTranslateInstance(-0.5, -0.5));
                return ProjectiveTransform.create(tr);
            }

            if(gridToWorld instanceof IdentityTransform){
                final AffineTransform tr= new AffineTransform(1,0,0,1,0,0);
                tr.concatenate(AffineTransform.getTranslateInstance(-0.5,-0.5));
                return ProjectiveTransform.create(tr);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("This reader's grid to world transform is invalid!");
    }
}