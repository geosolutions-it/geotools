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
package org.geotools.imageio.netcdf.cv;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.geotools.referencing.datum.DefaultEngineeringDatum;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.geotools.referencing.datum.DefaultPrimeMeridian;
import org.geotools.referencing.operation.DefaultOperationMethod;
import org.geotools.referencing.operation.DefiningConversion;
import org.geotools.referencing.operation.projection.MapProjection;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * Class used to properly setup NetCDF CRSs. 
 */
public class NetCDFCRSBuilder {

    /**
     * Cached {@link MathTransformFactory} for building {@link MathTransform}
     * objects.
     */
    private static final MathTransformFactory mtFactory;

//    private static final DatumFactory datumObjFactory;

    private static final String UNKNOWN = "unknown";

    static {
        Hints hints = GeoTools.getDefaultHints().clone();

//        // various authority related factories
//        allAuthoritiesFactory = new AllAuthoritiesFactory(hints);
//
//        // various factories
//        datumObjFactory = ReferencingFactoryFinder.getDatumFactory(hints);
        mtFactory = ReferencingFactoryFinder.getMathTransformFactory(hints);

    }

    /**
     * Extract the GridMapping information from the specified variable and setup a {@link CoordinateReferenceSystem} instance
     * @throws FactoryException 
     * */
    public static CoordinateReferenceSystem parseProjection() throws FactoryException {

        String projectionName = CF.LAMBERT_CONFORMAL_CONIC;
        boolean adjustLatitudeOfOrigin = false;
            projectionName = CF.LAMBERT_CONFORMAL_CONIC + "_2SP";
            adjustLatitudeOfOrigin = true;
        ParameterValueGroup parameters = mtFactory.getDefaultParameters(projectionName);

        // The NetCDF projection parameters
//        Map<String, String> projectionParams = projection.getParameters();
//        Set<String> parameterKeys = projectionParams.keySet();
//        for (String parameterKey: parameterKeys) {
//            handleParam(projectionParams, parameters, parameterKey, var);
//        }

        Unit<Length> linearUnit = SI.METER; //allAuthoritiesFactory.createUnit("EPSG:" + METER_UNIT_CODE);
        Unit<Angle> angularUnit = NonSI.DEGREE_ANGLE;

        //TODO: check for custom ellipsoids, prime meridian and datums
        // Ellipsoid and PrimeMeridian
        Ellipsoid ellipsoid = buildEllipsoid(6371229.0, linearUnit);
        PrimeMeridian primeMeridian = DefaultPrimeMeridian.GREENWICH;

        // Datum
        final GeodeticDatum datum = new DefaultGeodeticDatum(UNKNOWN, ellipsoid, primeMeridian);

        // Base Geographic CRS
        final Map<String, String> props = new HashMap<String, String>();

        // make the user defined GCS from all the components...
        props.put("name", UNKNOWN);
        GeographicCRS baseCRS = new DefaultGeographicCRS(props, datum,
                DefaultEllipsoidalCS.GEODETIC_2D.usingUnit(angularUnit));

        double semiMajor = ellipsoid.getSemiMajorAxis();
        double inverseFlattening = ellipsoid.getInverseFlattening();

        // setting missing parameters
        parameters.parameter("latitude_of_origin").setValue(0);
        parameters.parameter("standard_parallel_1").setValue(25);
        parameters.parameter("standard_parallel_2").setValue(25);
        parameters.parameter("central_meridian").setValue(-95);
        parameters.parameter("false_easting").setValue(0);
        parameters.parameter("false_northing").setValue(0);
        parameters.parameter("semi_minor").setValue(semiMajor * (1 - (1 / inverseFlattening)));
        parameters.parameter("semi_major").setValue(semiMajor);
//        if (adjustLatitudeOfOrigin) {
//            // TODO: check this setting. Without this, grib coverages are in the wrong geo-place
//            parameters.parameter("latitude_of_origin").setValue(0);
//        }

        // create math transform
        MathTransform transform = mtFactory.createParameterizedTransform(parameters);

        // create the projection transform
        DefiningConversion conversionFromBase= new DefiningConversion(
                Collections.singletonMap("name", UNKNOWN), 
                new DefaultOperationMethod(transform),
                transform);

        // Create the projected CRS
        return new DefaultProjectedCRS(
                buildProperties(projectionName, Citations.EPSG,  "971801"),
                conversionFromBase,
                baseCRS,
                transform,
                DefaultCartesianCS.PROJECTED);
    }

     
    

static Map<String, ?> buildProperties(String name, Citation authority, String code) {
Map<String, Object> props = new HashMap<String, Object>();
props.put(IdentifiedObject.NAME_KEY, name);
props.put(IdentifiedObject.IDENTIFIERS_KEY, new NamedIdentifier(authority, code));
return props;
}


    /**
     * Build a custom ellipsoid, looking for definition parameters
     * @param var
     * @param linearUnit
     * @return
     */
    private static Ellipsoid buildEllipsoid(Double earthRadius, Unit<Length> linearUnit) {
//        Number semiMajorAxis = null;
//        Number semiMinorAxis = null;
        Double inverseFlattening = Double.NEGATIVE_INFINITY;
        Ellipsoid ellipsoid = null;

//        // Look for semiMajorAxis first
//        Attribute semiMajorAxisAttribute = var.findAttribute(CF.SEMI_MAJOR_AXIS);
//        if (semiMajorAxisAttribute != null) {
//            semiMajorAxis = semiMajorAxisAttribute.getNumericValue();
//        }
//
//        // If not present, maybe it's a sphere. Looking for the radius
//        if (semiMajorAxis == null) {
//            semiMajorAxisAttribute = var.findAttribute(CF.EARTH_RADIUS);
//            if (semiMajorAxisAttribute != null) {
//                semiMajorAxis = semiMajorAxisAttribute.getNumericValue();
//            }
//        }
//
//        // Looking for semiMininorAxis
//        Double semiMajor = semiMajorAxis.doubleValue();
//        Attribute semiMinorAxisAttribute = var.findAttribute(CF.SEMI_MINOR_AXIS);
//        if (semiMinorAxisAttribute != null) {
//            semiMinorAxis = semiMinorAxisAttribute.getNumericValue();
//        }
//
//        // Create an Ellipsoid in case semiMinorAxis is defined
//        if (semiMinorAxis != null) {
//            ellipsoid = DefaultEllipsoid.createEllipsoid(UNKNOWN, semiMajor, semiMinorAxis.doubleValue()
//                , linearUnit);
//        }

        // If not defined yet, looking for other attributes.
//        if (ellipsoid == null) {
//            Attribute inverseFlatteningAttribute = var.findAttribute(CF.INVERSE_FLATTENING);
//            if (inverseFlatteningAttribute != null) {
//                inverseFlattening = inverseFlatteningAttribute.getNumericValue().doubleValue();
//            }
            ellipsoid = DefaultEllipsoid.createFlattenedSphere(UNKNOWN, earthRadius, inverseFlattening, linearUnit);
//        }
        return ellipsoid;
    }
}