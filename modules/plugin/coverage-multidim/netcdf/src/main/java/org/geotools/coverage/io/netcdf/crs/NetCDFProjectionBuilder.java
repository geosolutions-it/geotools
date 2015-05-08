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

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.geotools.referencing.datum.DefaultPrimeMeridian;
import org.geotools.referencing.operation.DefaultOperationMethod;
import org.geotools.referencing.operation.DefiningConversion;
import org.geotools.util.Utilities;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

/**
 * Class used to create an OGC {@link ProjectedCRS} instance on top 
 * of Projection name, parameters and Ellipsoid.
 * A default datum will be created on top of that ellipsoid.
 */
public class NetCDFProjectionBuilder {

    private static final String NAME = "name";

    /**
     * Cached {@link MathTransformFactory} for building {@link MathTransform} objects.
     */
    private static final MathTransformFactory mtFactory;

    public static final EllipsoidalCS DEFAULT_ELLIPSOIDAL_CS = DefaultEllipsoidalCS.GEODETIC_2D
            .usingUnit(NonSI.DEGREE_ANGLE);

    static {
        Hints hints = GeoTools.getDefaultHints().clone();

        mtFactory = ReferencingFactoryFinder.getMathTransformFactory(hints);

    }

    /**
     * Quick method to create a {@link CoordinateReferenceSystem} instance,
     * given the OGC ProjectionName, such as "lambert_conformal_conic_2sp"), 
     * a custom code number for it, the earthRadius (assuming the reference 
     * ellipsoid is a spheroid), and the Projection Params through a 
     * <key,value> map (as an instance: <"central_meridian",-95>)  
     * 
     * @throws FactoryException
     * */
    public static CoordinateReferenceSystem createProjection(String projectionName, String code,
            Double earthRadius, Map<String, Double> params) throws FactoryException {

        ParameterValueGroup parameters = mtFactory.getDefaultParameters(projectionName);

        Ellipsoid ellipsoid = buildEllipsoid(earthRadius);

        // Datum
        Set<String> keys = params.keySet();
        for (String key : keys) {
            parameters.parameter(key).setValue(params.get(key));
        }
        return buildProjectedCRS(buildProperties(projectionName, Citations.EPSG, code), parameters,
                ellipsoid);
    }

    /**
     * Make sure to set missing parameters
     * 
     * @param parameters
     * @param ellipsoid
     */
    private static void refineParameters(ParameterValueGroup parameters, Ellipsoid ellipsoid) {

        double semiMajor = ellipsoid.getSemiMajorAxis();
        double inverseFlattening = ellipsoid.getInverseFlattening();

        // setting missing parameters
        parameters.parameter(NetCDFUtilities.SEMI_MINOR).setValue(
                semiMajor * (1 - (1 / inverseFlattening)));
        parameters.parameter(NetCDFUtilities.SEMI_MAJOR).setValue(semiMajor);
    }

    public static DefiningConversion buildConversionFromBase(String name, MathTransform transform) {
        // create the projection transform
        return new DefiningConversion(Collections.singletonMap(NAME, name),
                new DefaultOperationMethod(transform), transform);
    }

    static Map<String, ?> buildProperties(String name, Citation authority, String code) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(IdentifiedObject.NAME_KEY, name);
        props.put(IdentifiedObject.IDENTIFIERS_KEY, new NamedIdentifier(authority, code));
        return props;
    }

    /**
     * Build a Spheroid on provided its earthRadius.
     * @param earthRadius the earth radius 
     * @return
     */
    private static Ellipsoid buildEllipsoid(Double earthRadius) {
        Map<String, Number> ellipsoidParams = new HashMap<String, Number>();
        ellipsoidParams.put(NetCDFUtilities.SEMI_MAJOR, earthRadius);
        return buildEllipsoid(NetCDFUtilities.UNKNOWN, ellipsoidParams);
    }

    /**
     * Build a Default {@link GeodeticDatum} on top of a specific {@link Ellipsoid}
     * instance, using {@link DefaultPrimeMeridian#GREENWICH} as primeMeridian.
     * @param name
     * @param ellipsoid
     * @return
     */
    public static GeodeticDatum buildGeodeticDatum(String name, Ellipsoid ellipsoid) {
        return new DefaultGeodeticDatum(name, ellipsoid, DefaultPrimeMeridian.GREENWICH);
    }

    /**
     * Build a {@link GeographicCRS} given the name to be assigned and 
     * the {@link GeodeticDatum} to be used.
     * {@link EllipsoidalCS} is {@value #DEFAULT_ELLIPSOIDAL_CS}
     * 
     * @param name
     * @param datum
     * @param ellipsoidalCS
     * @return
     */

    public static GeographicCRS buildGeographicCRS(String name, GeodeticDatum datum) {
        return buildGeographicCRS(name, datum, DEFAULT_ELLIPSOIDAL_CS);
    }

    /**
     * Build a {@link GeographicCRS} given the name to be assigned, the {@link GeodeticDatum} to be used and the {@link EllipsoidalCS}.
     * 
     * @param name
     * @param datum
     * @param ellipsoidalCS
     * @return
     */
    public static GeographicCRS buildGeographicCRS(String name, GeodeticDatum datum,
            EllipsoidalCS ellipsoidalCS) {
        final Map<String, String> props = new HashMap<String, String>();
        props.put(NAME, name);
        return new DefaultGeographicCRS(props, datum, ellipsoidalCS);
    }

    /**
     * Build a {@link ProjectedCRS} given the base {@link GeographicCRS}, 
     * the {@link DefiningConversion} instance from Base as well as the
     * {@link MathTransform} from the base CRS to returned CRS.
     * The derivedCS is {@link DefaultCartesianCS#PROJECTED} by default.
     * @param props
     * @param baseCRS
     * @param conversionFromBase
     * @param transform
     * @return
     */
    public static CoordinateReferenceSystem buildProjectedCRS(Map<String, ?> props,
            GeographicCRS baseCRS, DefiningConversion conversionFromBase, MathTransform transform) {
        // Create the projected CRS
        return new DefaultProjectedCRS(props, conversionFromBase, baseCRS, transform,
                DefaultCartesianCS.PROJECTED);

    }

    /**
     * Build a custom {@link Ellipsoid} provided the name and a Map contains <key,number> parameters describing that ellipsoid. Supported params are
     * {@link #SEMI_MAJOR}, {@link #SEMI_MINOR}, {@link NetCDFUtilities#INVERSE_FLATTENING}
     * 
     * @param name
     * @param ellipsoidParams
     * @return
     */
    public static Ellipsoid buildEllipsoid(String name, Map<String, Number> ellipsoidParams) {
        Number semiMajor = null;
        Number semiMinor = null;
        Number inverseFlattening = Double.NEGATIVE_INFINITY;
        if (ellipsoidParams.containsKey(NetCDFUtilities.SEMI_MAJOR)) {
            semiMajor = ellipsoidParams.get(NetCDFUtilities.SEMI_MAJOR);
        }
        if (ellipsoidParams.containsKey(NetCDFUtilities.SEMI_MINOR)) {
            semiMinor = ellipsoidParams.get(NetCDFUtilities.SEMI_MINOR);
        }
        if (ellipsoidParams.containsKey(NetCDFUtilities.INVERSE_FLATTENING)) {
            inverseFlattening = ellipsoidParams.get(NetCDFUtilities.INVERSE_FLATTENING);
        }
        if (semiMinor != null) {
            return DefaultEllipsoid.createEllipsoid(name, semiMajor.doubleValue(),
                    semiMinor.doubleValue(), SI.METER);
        } else {
            return DefaultEllipsoid.createFlattenedSphere(name, semiMajor.doubleValue(),
                    inverseFlattening.doubleValue(), SI.METER);
        }
    }

    /**
     * Create a {@link ProjectedCRS} parsing Conversion parameters and Ellipsoid
     * 
     * @param props
     * @param parameters
     * @param ellipsoid
     * @return
     * @throws NoSuchIdentifierException
     * @throws FactoryException
     */
    public static CoordinateReferenceSystem buildProjectedCRS(Map<String, ?> props,
            ParameterValueGroup parameters, Ellipsoid ellipsoid) throws NoSuchIdentifierException,
            FactoryException {
        // Refine the parameters by adding the required ellipsoid's related params
        refineParameters(parameters, ellipsoid);

        // Datum
        final GeodeticDatum datum = NetCDFProjectionBuilder.buildGeodeticDatum(
                NetCDFUtilities.UNKNOWN, ellipsoid);

        // Base Geographic CRS
        GeographicCRS baseCRS = NetCDFProjectionBuilder.buildGeographicCRS(NetCDFUtilities.UNKNOWN,
                datum);

        // create math transform
        MathTransform transform = mtFactory.createParameterizedTransform(parameters);

        // create the projection transform
        DefiningConversion conversionFromBase = NetCDFProjectionBuilder.buildConversionFromBase(
                NetCDFUtilities.UNKNOWN, transform);

        return NetCDFProjectionBuilder.buildProjectedCRS(props, baseCRS, conversionFromBase,
                transform);
    }

    /**
     * Get a {@link ParameterValueGroup} parameters instance for the specified projectionName.
     * 
     * @param projectionName
     * @return
     * @throws NoSuchIdentifierException
     */
    public static ParameterValueGroup getDefaultparameters(String projectionName)
            throws NoSuchIdentifierException {
        Utilities.ensureNonNull("projectionName", projectionName);
        return mtFactory.getDefaultParameters(projectionName);
    }
}