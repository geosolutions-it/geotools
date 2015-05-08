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
package org.geotools.coverage.io.netcdf.crs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * Class used to properly setup NetCDF CF Projection parameters. Given a known Projection, 
 * it will take care of remapping the Projection's parameters to NetCDF CF GridMapping parameters.
 * 
 * @see <a href="http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings">NetCDF CF, Appendix
 *      F: Grid Mappings</a>
 */
public class NetCDFProjection {

    private final static java.util.logging.Logger LOGGER = Logger.getLogger(NetCDFProjection.class.toString());
    /**
     * Cached {@link MathTransformFactory} for building {@link MathTransform}
     * objects.
     */

    public NetCDFProjection(String projectionName, Map<String, String> parametersMapping) {
        this.name = projectionName;
        this.netCDFParametersMapping = Collections.unmodifiableMap(parametersMapping);
    }

    private Map<String, String> netCDFParametersMapping;

    private String name; 

    /**
     * Returns the underlying unmodifiable Referencing to NetCDF parameters mapping.
     * 
     * @return
     */
    public Map<String, String> getParameters() {
        return netCDFParametersMapping;
    }

    public String getName() {
        return name;
    }

    /**
     * Currently supported NetCDF projections. TODO: Add more. Check the CF Document
     * 
     * @see <a href="http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings">NetCDF CF,
     *      Appendix F: Grid Mappings</a>
     */
    public final static NetCDFProjection LAMBERT_AZIMUTHAL_EQUAL_AREA;
    public final static NetCDFProjection TRANSVERSE_MERCATOR;
    public final static NetCDFProjection ORTHOGRAPHIC;
    public final static NetCDFProjection POLAR_STEREOGRAPHIC;
    public final static NetCDFProjection STEREOGRAPHIC;
    public final static NetCDFProjection LAMBERT_CONFORMAL_CONIC_1SP;
    public final static NetCDFProjection LAMBERT_CONFORMAL_CONIC_2SP;

    /** The map of currently supported NetCDF CF Grid mappings */
    private final static Map<String, NetCDFProjection> supportedProjections = new HashMap<String, NetCDFProjection>();

    private static final String UNKNOWN = "unknown";

    static {

        // Setting up Lambert Azimuthal equal area
        Map<String, String> lazeq_mapping = new HashMap<String, String>();
        lazeq_mapping.put(NetCDFUtilities.CENTRAL_MERIDIAN, CF.LONGITUDE_OF_PROJECTION_ORIGIN);
        lazeq_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        lazeq_mapping.put(NetCDFUtilities.FALSE_EASTING, CF.FALSE_EASTING);
        lazeq_mapping.put(NetCDFUtilities.FALSE_NORTHING, CF.FALSE_NORTHING);
        LAMBERT_AZIMUTHAL_EQUAL_AREA = new NetCDFProjection(CF.LAMBERT_AZIMUTHAL_EQUAL_AREA, lazeq_mapping);

        // Setting up Transverse Mercator
        Map<String, String> tm_mapping = new HashMap<String, String>();
        tm_mapping.put(NetCDFUtilities.SCALE_FACTOR, CF.SCALE_FACTOR_AT_CENTRAL_MERIDIAN);
        tm_mapping.put(NetCDFUtilities.CENTRAL_MERIDIAN, CF.LONGITUDE_OF_CENTRAL_MERIDIAN);
        tm_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        tm_mapping.put(NetCDFUtilities.FALSE_EASTING, CF.FALSE_EASTING);
        tm_mapping.put(NetCDFUtilities.FALSE_NORTHING, CF.FALSE_NORTHING);
        TRANSVERSE_MERCATOR = new NetCDFProjection(CF.TRANSVERSE_MERCATOR, tm_mapping);

        // Setting up Orthographic
        Map<String, String> ortho_mapping = new HashMap<String, String>();
        ortho_mapping.put(NetCDFUtilities.CENTRAL_MERIDIAN, CF.LONGITUDE_OF_PROJECTION_ORIGIN);
        ortho_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        ortho_mapping.put(NetCDFUtilities.FALSE_EASTING, CF.FALSE_EASTING);
        ortho_mapping.put(NetCDFUtilities.FALSE_NORTHING, CF.FALSE_NORTHING);
        ORTHOGRAPHIC = new NetCDFProjection(CF.ORTHOGRAPHIC, ortho_mapping);

        // Setting up Polar Stereographic
        Map<String, String> polarstereo_mapping = new HashMap<String, String>();
        polarstereo_mapping.put(NetCDFUtilities.CENTRAL_MERIDIAN, CF.STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE);
        polarstereo_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        polarstereo_mapping.put(NetCDFUtilities.SCALE_FACTOR, CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN);
        polarstereo_mapping.put(NetCDFUtilities.FALSE_EASTING, CF.FALSE_EASTING);
        polarstereo_mapping.put(NetCDFUtilities.FALSE_NORTHING, CF.FALSE_NORTHING);
        POLAR_STEREOGRAPHIC = new NetCDFProjection(CF.POLAR_STEREOGRAPHIC, polarstereo_mapping);

        // Setting up Stereographic
        Map<String, String> stereo_mapping = new HashMap<String, String>();
        stereo_mapping.put(NetCDFUtilities.CENTRAL_MERIDIAN, CF.LONGITUDE_OF_PROJECTION_ORIGIN);
        stereo_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        stereo_mapping.put(NetCDFUtilities.SCALE_FACTOR, CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN);
        stereo_mapping.put(NetCDFUtilities.FALSE_EASTING, CF.FALSE_EASTING);
        stereo_mapping.put(NetCDFUtilities.FALSE_NORTHING, CF.FALSE_NORTHING);
        STEREOGRAPHIC = new NetCDFProjection(CF.STEREOGRAPHIC, stereo_mapping);

        Map<String, String> lcc_mapping = new HashMap<String, String>();

        lcc_mapping.put(NetCDFUtilities.CENTRAL_MERIDIAN, CF.LONGITUDE_OF_CENTRAL_MERIDIAN);
        lcc_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        lcc_mapping.put(NetCDFUtilities.FALSE_EASTING, CF.FALSE_EASTING);
        lcc_mapping.put(NetCDFUtilities.FALSE_NORTHING, CF.FALSE_NORTHING);

        // Setting up Lambert Conformal Conic 1SP
        Map<String, String> lcc_1sp_mapping = new HashMap<String, String>();
        lcc_1sp_mapping.putAll(lcc_mapping);
        lcc_1sp_mapping.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, CF.STANDARD_PARALLEL);
        LAMBERT_CONFORMAL_CONIC_1SP = new NetCDFProjection(CF.LAMBERT_CONFORMAL_CONIC, lcc_1sp_mapping);

        // Setting up Lambert Conformal Conic 2SP
        Map<String, String> lcc_2sp_mapping = new HashMap<String, String>();
        lcc_2sp_mapping.putAll(lcc_mapping);
        lcc_2sp_mapping.put(NetCDFUtilities.STANDARD_PARALLEL_1, CF.STANDARD_PARALLEL);
        lcc_2sp_mapping.put(NetCDFUtilities.STANDARD_PARALLEL_2, CF.STANDARD_PARALLEL);
        LAMBERT_CONFORMAL_CONIC_2SP = new NetCDFProjection(CF.LAMBERT_CONFORMAL_CONIC, lcc_2sp_mapping);

        supportedProjections.put(TRANSVERSE_MERCATOR.name, TRANSVERSE_MERCATOR);
        supportedProjections.put(CF.LAMBERT_CONFORMAL_CONIC + "_1SP", LAMBERT_CONFORMAL_CONIC_1SP);
        supportedProjections.put(CF.LAMBERT_CONFORMAL_CONIC + "_2SP", LAMBERT_CONFORMAL_CONIC_2SP);
        supportedProjections.put(LAMBERT_AZIMUTHAL_EQUAL_AREA.name, LAMBERT_AZIMUTHAL_EQUAL_AREA);
        supportedProjections.put(ORTHOGRAPHIC.name, ORTHOGRAPHIC);
        supportedProjections.put(POLAR_STEREOGRAPHIC.name, POLAR_STEREOGRAPHIC);
        supportedProjections.put(STEREOGRAPHIC.name, STEREOGRAPHIC);

        // TODO:
        //    ALBERS_EQUAL_AREA, AZIMUTHAL_EQUIDISTANT,  LAMBERT_CONFORMAL, LAMBERT_CYLINDRICAL_EQUAL_AREA, MERCATOR,
        //    , ROTATED_POLE, STEREOGRAPHIC,
    }

    /** 
     * Get a NetCDF Projection definition referred by name 
     */
    public static NetCDFProjection getSupportedProjection(String projectionName) {
        if (supportedProjections.containsKey(projectionName)) {
            return supportedProjections.get(projectionName);
        } else {
            LOGGER.warning("The specified projection isn't currently supported: " + projectionName);
            return null;
        }
    }

    /**
     * Extract the GridMapping information from the specified variable and setup a {@link CoordinateReferenceSystem} instance
     * @throws FactoryException 
     * */
    public static CoordinateReferenceSystem parseProjection(Variable var) throws FactoryException {
        // Preliminar check on spatial_ref attribute which may contain a fully defined WKT
        // as an instance, being set from GDAL, or a GeoTools NetCDF ouput 

        Attribute spatialRef = var.findAttribute(NetCDFUtilities.SPATIAL_REF);
        CoordinateReferenceSystem crs = parseSpatialRef(spatialRef);
        if (crs != null) {
            return crs;
        }

        Attribute gridMappingName = var.findAttribute(NetCDFUtilities.GRID_MAPPING_NAME);
        if (gridMappingName == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("No grid_mapping_name attribute has been found. Unable to parse a CF projection from this variable");
            }
            return null;
        }

        // Preliminar checks on special cases 
        String mappingName = gridMappingName.getStringValue();
        String projectionName = mappingName;
        if (mappingName.equalsIgnoreCase(CF.LAMBERT_CONFORMAL_CONIC)) {
            Attribute standardParallel = var.findAttribute(CF.STANDARD_PARALLEL);
            final int numParallels = standardParallel.getLength();
            projectionName = CF.LAMBERT_CONFORMAL_CONIC + (numParallels == 1 ? "_1SP" : "_2SP");
        }

        // Getting the proper projection and set the projection parameters
        NetCDFProjection projection = supportedProjections.get(projectionName);

        // The GT referencing projection parameters
        ParameterValueGroup parameters = NetCDFProjectionBuilder.getDefaultparameters(projectionName);

        // The NetCDF projection parameters
        Map<String, String> projectionParams = projection.getParameters();
        Set<String> parameterKeys = projectionParams.keySet();
        for (String parameterKey: parameterKeys) {
            handleParam(projectionParams, parameters, parameterKey, var);
        }

        // Ellipsoid
        Ellipsoid ellipsoid = buildEllipsoid(var, SI.METER);
        return NetCDFProjectionBuilder.buildProjectedCRS(java.util.Collections.singletonMap("name", projectionName), parameters, ellipsoid);
    }

    private static void handleParam(Map<String, String> projectionParams, ParameterValueGroup parameters, String parameterKey,
            Variable var) {
        String attributeName = projectionParams.get(parameterKey);
        Double value = null;
        if (parameterKey.equalsIgnoreCase(NetCDFUtilities.STANDARD_PARALLEL_1)
                || parameterKey.equalsIgnoreCase(NetCDFUtilities.STANDARD_PARALLEL_2)) {
            Attribute attribute = var.findAttribute(attributeName);
            if (attribute != null) {
                final int numValues = attribute.getLength();
                if (numValues > 1) {
                    int index = parameterKey.equalsIgnoreCase(NetCDFUtilities.STANDARD_PARALLEL_1) ? 0 : 1;
                    Number number = (Number) attribute.getValue(index);
                    value = number.doubleValue();
                } else {
                    value = attribute.getNumericValue().doubleValue();
                }
            }
        } else {

            Attribute attribute = var.findAttribute(attributeName);
            if (attribute != null) {
                // Get the parameter value and handle special management for longitudes outside -180, 180
                value = attribute.getNumericValue().doubleValue();
                if (attributeName.contains("meridian") || attributeName.contains("longitude")) {
                    value = value - (360) * Math.floor(value / (360) + 0.5);
                }
            }
        }
        if (value != null) {
            parameters.parameter(parameterKey).setValue(value);
        }
    }

    /** 
     * Extract the {@link CoordinateReferenceSystem} from the 
     * {@link NetCDFUtilities#SPATIAL_REF} attribute if present.
     * @param spatialRef the NetCDF SPATIAL_REF {@link Attribute}
     * @return
     */
    private static CoordinateReferenceSystem parseSpatialRef(Attribute spatialRef) {
        CoordinateReferenceSystem crs = null;
        if (spatialRef != null) {
            String wkt = spatialRef.getStringValue();
            try {
                crs = CRS.parseWKT(wkt);
            } catch (FactoryException e) {
                if (LOGGER.isLoggable(Level.WARNING)){ 
                    LOGGER.warning("Unable to setup a CRS from the specified WKT: " + wkt);
                }
            }
        }
        return crs;
    }

    /**
     * Build a custom ellipsoid, looking for definition parameters from a 
     * GridMapping variable
     * @param gridMappingVariable the variable to be analyzed
     * @param linearUnit the linear Unit to be used for the ellipsoid 
     * @return
     */
    private static Ellipsoid buildEllipsoid(Variable gridMappingVariable, Unit<Length> linearUnit) {
        Number semiMajorAxis = null;
        Number semiMinorAxis = null;
        Double inverseFlattening = Double.NEGATIVE_INFINITY;

        // Preparing ellipsoid params to be sent to the NetCDFProjectionBuilder class
        // in order to get back an Ellipsoid

        Map<String, Number> ellipsoidParams = new HashMap<String, Number>();

        // Looking for semiMajorAxis first
        Attribute semiMajorAxisAttribute = gridMappingVariable.findAttribute(CF.SEMI_MAJOR_AXIS);
        if (semiMajorAxisAttribute != null) {
            semiMajorAxis = semiMajorAxisAttribute.getNumericValue();
            ellipsoidParams.put(NetCDFUtilities.SEMI_MAJOR, semiMajorAxis); 
        }

        // If not present, maybe it's a sphere. Looking for the radius
        if (semiMajorAxis == null) {
            semiMajorAxisAttribute = gridMappingVariable.findAttribute(CF.EARTH_RADIUS);
            if (semiMajorAxisAttribute != null) {
                semiMajorAxis = semiMajorAxisAttribute.getNumericValue();
                ellipsoidParams.put(NetCDFUtilities.SEMI_MAJOR, semiMajorAxis);
            }
        }

        // Looking for semiMinorAxis
        Attribute semiMinorAxisAttribute = gridMappingVariable.findAttribute(CF.SEMI_MINOR_AXIS);
        if (semiMinorAxisAttribute != null) {
            semiMinorAxis = semiMinorAxisAttribute.getNumericValue();
            ellipsoidParams.put(NetCDFUtilities.SEMI_MINOR, semiMinorAxis);
        }

        if (semiMinorAxis == null) {
            // Looking for inverse Flattening
            Attribute inverseFlatteningAttribute = gridMappingVariable.findAttribute(CF.INVERSE_FLATTENING);
            if (inverseFlatteningAttribute != null) {
                inverseFlattening = inverseFlatteningAttribute.getNumericValue().doubleValue();
            }
            ellipsoidParams.put(NetCDFUtilities.INVERSE_FLATTENING, inverseFlattening);
        }

        // Ellipsoid parameters have been set. Getting back an Ellipsoid from the 
        // builder
        return NetCDFProjectionBuilder.buildEllipsoid(UNKNOWN, ellipsoidParams);
    }

    /**
     * Look for a SPATIAL_REF global attribute and parsing it (as WKT) 
     * to setup a {@link CoordinateReferenceSystem}
     * @param dataset
     * @return
     */
    public static CoordinateReferenceSystem parseProjection(NetcdfDataset dataset) {
        Attribute attribute = dataset.findAttribute(NetCDFUtilities.SPATIAL_REF);
        return parseSpatialRef(attribute);
    }
}