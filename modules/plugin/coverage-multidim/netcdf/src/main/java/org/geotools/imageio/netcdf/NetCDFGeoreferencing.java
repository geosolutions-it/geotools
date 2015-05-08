/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.imageio.netcdf;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.io.netcdf.NetCDFCRSAuthorityFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.imageio.netcdf.cv.CoordinateVariable;
import org.geotools.imageio.netcdf.cv.NetCDFProjection;
import org.geotools.imageio.netcdf.utilities.NetCDFCRSUtilities;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.IdentifiedObjectFinder;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Store information about the underlying NetCDF georeferencing.
 */
class NetCDFGeoreferencing {

    private static AbstractAuthorityFactory crsFactory;
    
    static {
        for (final CRSAuthorityFactory factory : ReferencingFactoryFinder
                .getCRSAuthorityFactories(null/*FORCE_LONGITUDE_FIRST_AXIS_ORDER*/))
    {
        final AbstractAuthorityFactory f = (AbstractAuthorityFactory) factory;
        if (f instanceof NetCDFCRSAuthorityFactory) {
            crsFactory = f;
            break;
        }
    }
    }
    
    private final static Logger LOGGER = Logging.getLogger(NetCDFGeoreferencing.class.toString());
    
    /**
     * Set it to {@code true} in case the dataset contains multiple 2D coordinates definitions. Used to quickly access the bbox in case there is only
     * a single one.
     */
    private boolean hasMultiple2Dcoords;

    /** Mapping containing the relation between a dimension and the related coordinate variable */
    private Map<String, String> dimensionsMapping;

    /** Map containing coordinates being wrapped by variables */
    private Map<String, CoordinateVariable<?>> coordinatesVariables;

    /**
     * BoundingBoxes available for the underlying dataset. Most common case is that all the dataset has a single boundingbox/grid/mapping. This will
     * be signaled by the {@link #hasMultiple2Dcoords} flag equal to {@code false}. In that case, the map will only contain a single bbox which mapped
     * to the "DEFAULT" name
     */
    private Map<String, ReferencedEnvelope> boundingBoxes = new HashMap<String, ReferencedEnvelope>();

    final static String DEFAULT = "Default";

    /** Boolean indicating if the input file needs flipping or not */
    private boolean needsFlipping = false;

    /** The underlying NetCDF dataset */
    private NetcdfDataset dataset;

    public boolean isNeedsFlipping() {
        return needsFlipping;
    }

    public void setNeedsFlipping(boolean needsFlipping) {
        this.needsFlipping = needsFlipping;
    }

    public boolean isHasMultiple2Dcoords() {
        return hasMultiple2Dcoords;
    }

    public void setHasMultiple2Dcoords(boolean hasMultiple2Dcoords) {
        this.hasMultiple2Dcoords = hasMultiple2Dcoords;
    }

    public CoordinateVariable<?> getCoordinateVariable(String name) {
        return coordinatesVariables.get(name);
    }

    public Collection<CoordinateVariable<?>> getCoordinatesVariables() {
        return coordinatesVariables.values();
    }

    public void addBoundingBox(String mapName, ReferencedEnvelope boundingBox) {
        boundingBoxes.put(mapName, boundingBox);
    }

    public void dispose() {
        if (coordinatesVariables != null) {
            coordinatesVariables.clear();
        }
        if (boundingBoxes != null) {
            boundingBoxes.clear();
        }
    }

    public ReferencedEnvelope getBoundingBox(String shortName) {
        if (!hasMultiple2Dcoords) {
            return boundingBoxes.get(DEFAULT);
        }

        // TODO: ADD support for multiple 2D coordinates definitions within the same dataset
        return null;
    }

    public Collection<CoordinateVariable<?>> getCoordinatesVariables(String shortName) {
        if (!hasMultiple2Dcoords) {
            return coordinatesVariables.values();
        }

        // TODO: ADD support for multiple 2D coordinates definitions within the same dataset
        return null;
    }

    /** 
     * Main constructor to setup the NetCDF Georeferencing based on the available
     * information stored within the NetCDF dataset. 
     * */
    public NetCDFGeoreferencing(NetcdfDataset dataset) {
        this.dataset = dataset;
        extractCoordinatesVariable();
        try {
            extractBBOX();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (FactoryException fe) {
            throw new RuntimeException(fe);
        }
    }

    /**
     * Parse the CoordinateAxes of the dataset and setup proper {@link CoordinateVariable} instances
     * on top of it.
     */
    private void extractCoordinatesVariable() {
        // get the coordinate variables
        Map<String, CoordinateVariable<?>> coordinates = new HashMap<String, CoordinateVariable<?>>();
        for (CoordinateAxis axis : dataset.getCoordinateAxes()) {
            if (axis instanceof CoordinateAxis1D && axis.getAxisType() != null) {
                coordinates.put(axis.getFullName(), CoordinateVariable.create((CoordinateAxis1D)axis));
            } else {
                // Workaround for Unsupported Axes
                Set<String> unsupported = NetCDFUtilities.getUnsupportedDimensions();
                if (axis instanceof CoordinateAxis1D && unsupported.contains(axis.getFullName())) {
                    axis.setAxisType(AxisType.GeoZ);
                    coordinates.put(axis.getFullName(),
                            CoordinateVariable.create((CoordinateAxis1D) axis));
                // Workaround for files that have a time dimension, but in a format that could not be parsed
                } else if ("time".equals(axis.getFullName())) {
                    LOGGER.warning("Detected unparseable unit string in time axis: '"
                            + axis.getUnitsString() + "'.");
                    axis.setAxisType(AxisType.Time);
                    coordinates.put(axis.getFullName(),
                            CoordinateVariable.create((CoordinateAxis1D) axis));
                } else {
                    LOGGER.warning("Unsupported axis: " + axis + " in input: " + dataset.getLocation()
                            + " has been found");
                }
            }
        }
        coordinatesVariables = coordinates;
        initMapping(dataset.getCoordinateAxes());
    }

    /**
     * Extract the bbox information
     * 
     * @throws IOException 
     * @throws FactoryException 
     */
    private void extractBBOX() throws IOException, FactoryException {
        double [] lon= new double[2];
        double [] lat= new double[2];
        byte set=0;
        for (CoordinateVariable<?> cv : getCoordinatesVariables()) {
            if (cv.isNumeric()) {

                // is it lat or lon?
                AxisType type = cv.getAxisType();
                switch (type) {
                case GeoY: case Lat:
                    if (cv.isRegular()) {
                        lat[0] = cv.getStart() - (cv.getIncrement() / 2d);
                        lat[1] = lat[0] + cv.getIncrement() * (cv.getSize());
                    } else {
                        double min = ((Number) cv.getMinimum()).doubleValue();
                        double max = ((Number) cv.getMaximum()).doubleValue();
                        double incr = (max - min) / (cv.getSize() - 1);
                        lat[0] = min - (incr / 2d);
                        lat[1] = max + (incr / 2d);
                    }
                    if (lat[1] > lat[0]) {
                        setNeedsFlipping(true);
                    } else {
                        setNeedsFlipping(false);
                    }
                    set++;
                    break;
                case GeoX:
                case Lon:
                    if (cv.isRegular()) {
                        lon[0] = cv.getStart() - (cv.getIncrement() / 2d);
                        lon[1] = lon[0] + cv.getIncrement() * (cv.getSize());
                    } else {
                        double min = ((Number) cv.getMinimum()).doubleValue();
                        double max = ((Number) cv.getMaximum()).doubleValue();
                        double incr = (max - min) / (cv.getSize() - 1);
                        lon[0] = min - (incr / 2d);
                        lon[1] = max + (incr / 2d);
                    }
                    set++;
                    break;
                default:
                    break;
                }
            }
            if (set == 2) {
                break;
            }
        }
        // create the envelope
        if (set != 2) {
            throw new IllegalStateException("Unable to create envelope for this dataset");
        }
        CoordinateReferenceSystem crs = NetCDFCRSUtilities.WGS84;
        if (!isHasMultiple2Dcoords()) {

            // Looks for gridMapping
            List<Variable> variables = dataset.getVariables();
            boolean projectionSet = false; 
            for (Variable variable: variables) {

                // TODO: Support for multiple coordinates 2D definitions within the same dataset
                Attribute attrib = variable.findAttribute(NetCDFUtilities.GRID_MAPPING_NAME);
                if (attrib != null) {
                    // Grid Mapping found
                    crs = NetCDFProjection.parseProjection(variable);
                    projectionSet = true;
                    break;
                }
            }
            if (!projectionSet) {
                CoordinateReferenceSystem projection = NetCDFProjection.parseProjection(dataset);
                if (projection != null) {
                    projectionSet = true;
                    crs = projection;
                }
            }
            if (projectionSet) {
                // Force EPSG crs
                IdentifiedObjectFinder finder = crsFactory.getIdentifiedObjectFinder(crs.getClass());
                finder.setFullScanAllowed(true);
                final String code = finder.findIdentifier(crs);
                if (code != null) {
                    crs = CRS.decode(code); 
                }
            }
            ReferencedEnvelope boundingBox = new ReferencedEnvelope(lon[0], lon[1], lat[0], lat[1], crs);
            addBoundingBox(NetCDFGeoreferencing.DEFAULT, boundingBox);
            
        } else {
            //TODO: Support multiple Grids definition within the same file
        }
    }

    /**
     * Parse the coordinateAxes and retrieve the associated coordinateVariable to be used for the
     * dimension mapping.
     *  
     * @param coordinateAxes
     */
    private void initMapping(List<CoordinateAxis> coordinateAxes) {
        // check other dimensions
        int coordinates2D = 0;
        Map<String, String> dimensionsMap = new HashMap<String, String>();
        for (CoordinateAxis axis : coordinateAxes) {
            // get from coordinate vars
            final CoordinateVariable<?> cv = getCoordinateVariable(axis.getFullName());
            if (cv != null) {
                final String name = cv.getName();
                switch(cv.getAxisType()){
                    case GeoX: case GeoY: case Lat: case Lon:
                        // TODO: Add support for multiple different lon/lat,x/y coordinates within the same file
                        coordinates2D++;
                        continue;
                    case Height: case Pressure: case RadialElevation: case RadialDistance: case GeoZ:
                        if (NetCDFCRSUtilities.VERTICAL_AXIS_NAMES.contains(name) && !dimensionsMap.containsKey(NetCDFUtilities.ELEVATION_DIM)) {
                            // Main elevation dimension
                            dimensionsMap.put(NetCDFUtilities.ELEVATION_DIM, name);
                        } else {
                            // additional elevation dimension
                            dimensionsMap.put(name.toUpperCase(), name);
                        }
                        break;
                    case Time:
                        if (!dimensionsMap.containsKey(NetCDFUtilities.TIME_DIM)) {
                            // Main time dimension
                            dimensionsMap.put(NetCDFUtilities.TIME_DIM, name);
                        } else {
                            // additional time dimension
                            dimensionsMap.put(name.toUpperCase(), name);
                        }
                        break;
                }
            }else {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Null coordinate variable: '" + axis.getFullName() + "' while processing input: " + dataset.getLocation());
                }
            }
        }
        if (coordinates2D > 2) {
            setHasMultiple2Dcoords(true);
        }

        dimensionsMapping = dimensionsMap;
    }

    /**
     * Return the whole dimension to coordinateVariable mapping
     */
    public Map<String, String> getDimensions() {
        return dimensionsMapping;
    }

    /** 
     * Return the dimension name associated to the specified coordinateVariable.
     */
    public String getDimension(String coordinateVariableName) {
        return dimensionsMapping.get(coordinateVariableName);
    }

}
