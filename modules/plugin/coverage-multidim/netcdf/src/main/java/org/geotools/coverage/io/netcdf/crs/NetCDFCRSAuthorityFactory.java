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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.factory.Hints;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.factory.DirectAuthorityFactory;
import org.geotools.referencing.factory.epsg.ThreadedEpsgFactory;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.util.SimpleInternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.util.InternationalString;

import ucar.nc2.constants.CF;

/**
 * A factory providing NetCDF/GRIB custom {@link CoordinateReferenceSystem} 
 * instances with the related custom EPSG.
 * 
 * @author Daniele Romagnoli - GeoSolutions
 *
 */
public class NetCDFCRSAuthorityFactory extends DirectAuthorityFactory implements
        CRSAuthorityFactory {

    private static final String EPSG_PREFIX = "EPSG:";

    private static final int EPSG_CUT = EPSG_PREFIX.length();

    /**
     * Simple holder class storing an EPSG Code, as well as the related 
     * {@link CoordinateReferenceSystem} custom instance.
     */
    static class CoordinateReferenceSystemInfo {
        public CoordinateReferenceSystemInfo(int numberCode, String description) {
            this.numberCode = Integer.toString(numberCode);
            this.epsgCode = EPSG_PREFIX + numberCode;
            this.description = new SimpleInternationalString(description);
        }

        /** numeric code */
        String numberCode;

        /** numeric code prefixed by "EPSG:" */
        String epsgCode;

        /** A description for this coordinate reference system */
        InternationalString description;

        /** custom {@link CoordinateReferenceSystem} instance */
        CoordinateReferenceSystem crs;

        public String getNumberCode() {
            return numberCode;
        }

        public String getEpsgCode() {
            return epsgCode;
        }

        public InternationalString getDescription() {
            return description;
        }

        public CoordinateReferenceSystem getCrs() {
            return crs;
        }

        public void setCrs(CoordinateReferenceSystem crs) {
            this.crs = crs;
        }
    }

    static final int GRIB_CIP_CRS_CODE = 971801;

    static Map<String, CoordinateReferenceSystemInfo> CUSTOM_CRSS = new HashMap<String, CoordinateReferenceSystemInfo>();

    static Set<String> CODES = new HashSet<String>();

    static Set<String> EPSG_CODES = new HashSet<String>();
    static {
        try {
            // TODO: Parse them from a dictionary
            Map<String, Double> parameters = new HashMap<String, Double>();
            parameters.put(NetCDFUtilities.LATITUDE_OF_ORIGIN, 25d);
            parameters.put(NetCDFUtilities.CENTRAL_MERIDIAN, -95d);
            parameters.put(NetCDFUtilities.FALSE_EASTING, 0d);
            parameters.put(NetCDFUtilities.FALSE_NORTHING, 0d);
            CoordinateReferenceSystemInfo gribCipCrsInfo = new CoordinateReferenceSystemInfo(
                    GRIB_CIP_CRS_CODE, "A LambertConformalConic Projection for GRIB CIP models");
            String gribNumberCode = gribCipCrsInfo.getNumberCode();
            String epsgCode = gribCipCrsInfo.getEpsgCode();
            CoordinateReferenceSystem gribCipCRS = NetCDFProjectionBuilder.createProjection(
                    CF.LAMBERT_CONFORMAL_CONIC + "_1SP", gribNumberCode, 6371229.0, parameters);
            gribCipCrsInfo.setCrs(gribCipCRS);
            CUSTOM_CRSS.put(gribNumberCode, gribCipCrsInfo);
            CODES.add(gribNumberCode);
            EPSG_CODES.add(epsgCode);

        } catch (FactoryException e) {

        }
    }

    static Map<String, ?> buildProperties(String name, Citation authority, String code) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(IdentifiedObject.NAME_KEY, name);
        props.put(IdentifiedObject.IDENTIFIERS_KEY, new NamedIdentifier(authority, code));
        return props;
    }

    public NetCDFCRSAuthorityFactory() {
        this(null);
    }

    public NetCDFCRSAuthorityFactory(Hints hints) {
        super(hints, ThreadedEpsgFactory.MAXIMUM_PRIORITY - 5);
    }

    @Override
    public Citation getAuthority() {
        return Citations.EPSG;
    }

    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type)
            throws FactoryException {
        if (type.isAssignableFrom(ProjectedCRS.class)) {
            final Set set = new LinkedHashSet();
            set.addAll(CODES);
            return set;
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public InternationalString getDescriptionText(String code) throws NoSuchAuthorityCodeException,
            FactoryException {
        if (EPSG_CODES.contains(code)) {
            return CUSTOM_CRSS.get(cutEpsg(code)).getDescription();
        } else {
            throw noSuchAuthorityException(code);
        }
    }

    /**
     * Creates an object from the specified code. The default implementation delegates to
     * <code>{@linkplain #createCoordinateReferenceSystem createCoordinateReferenceSystem}(code)</code> .
     */
    public IdentifiedObject createObject(final String code) throws FactoryException {
        return createCoordinateReferenceSystem(code);
    }

    /**
     * Creates a coordinate reference system from the specified code.
     */
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code)
            throws FactoryException {
        if (CODES.contains(code)) {
            return CUSTOM_CRSS.get(code).getCrs();
        } else if (code.startsWith(EPSG_PREFIX)) {
            return createCoordinateReferenceSystem(cutEpsg(code));
        } else {
            throw noSuchAuthorityException(code);
        }
    }

    private String cutEpsg(String code) {
        // This method is invoked only after checking that the code
        // starts with "EPSG:"
        // We avoid NPE checks and startsWith checks
        return code.substring(EPSG_CUT);
    }

    private NoSuchAuthorityCodeException noSuchAuthorityException(String code)
            throws NoSuchAuthorityCodeException {
        String authority = EPSG_PREFIX;
        return new NoSuchAuthorityCodeException(Errors.format(ErrorKeys.NO_SUCH_AUTHORITY_CODE_$3,
                code, authority, ProjectedCRS.class), authority, code);
    }

}
