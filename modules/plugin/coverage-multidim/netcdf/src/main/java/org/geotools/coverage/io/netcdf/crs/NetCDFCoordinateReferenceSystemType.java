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

import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.projection.LambertAzimuthalEqualArea;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.LambertConformal2SP;
import org.geotools.referencing.operation.projection.Orthographic;
import org.geotools.referencing.operation.projection.PolarStereographic;
import org.geotools.referencing.operation.projection.Stereographic;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;

/**
 * Enum used to represent different coordinate reference systems stored within a NetCDF dataset.
 * NetCDF CF supports several types of projections through grid mapping.
 * 
 * Unsupported projections will be specified through the spatial_ref and GeoTransform global attributes
 * defined by GDAL. 
 * 
 * @see <a href="http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings">NetCDF CF, Appendix
 *      F: Grid Mappings</a>
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public enum NetCDFCoordinateReferenceSystemType {

    WGS84 {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.LATLON_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            // No projection/gridMapping is available for WGS84.
            // Coordinates can be used as is
            return null;
        };

    },
    SPATIAL_REF {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            // No Specific Projection is available for this type
            // We need to parse the SPATIAL_REF attribute
            // to setup a proper one
            return null;
        };
    },
    LAMBERT_AZIMUTHAL_EQUAL_AREA {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.LAMBERT_AZIMUTHAL_EQUAL_AREA;
        }
    },
    LAMBERT_CONFORMAL_CONIC_1SP {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.LAMBERT_CONFORMAL_CONIC_1SP;
        }
    },
    LAMBERT_CONFORMAL_CONIC_2SP {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.LAMBERT_CONFORMAL_CONIC_2SP;
        };
    },

    TRANSVERSE_MERCATOR {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.TRANSVERSE_MERCATOR;
        }
    },

    ORTHOGRAPHIC {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.ORTHOGRAPHIC;
        }
    },

    POLAR_STEREOGRAPHIC {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.POLAR_STEREOGRAPHIC;
        }
    },

    STEREOGRAPHIC {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjection getNetCDFProjection() {
            return NetCDFProjection.STEREOGRAPHIC;
        }
    }


    /* TODO: THESE CRSs still need to be added
     * , ALBERS_EQUAL_AREA, AZIMUTHAL_EQUIDISTANT,  LAMBERT_CONFORMAL, LAMBERT_CYLINDRICAL_EQUAL_AREA, MERCATOR,
     * , ROTATED_POLE, ,
     */;

     /**
      * Return a proper {@link NetCDFCoordinateReferenceSystemType} 
      * depending on the input OGC {@link CoordinateReferenceSystem} instance.
      * 
      * @param crs
      * @return
      */
    public static NetCDFCoordinateReferenceSystemType parseCRS(CoordinateReferenceSystem crs) {
        if (crs instanceof DefaultGeographicCRS) {
            return WGS84;
        } else if (crs instanceof ProjectedCRS) {
            ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            if (transform instanceof TransverseMercator) {
                return TRANSVERSE_MERCATOR;
            } else if (transform instanceof LambertConformal1SP) {
                return LAMBERT_CONFORMAL_CONIC_1SP;
            } else if (transform instanceof LambertConformal2SP) {
                return LAMBERT_CONFORMAL_CONIC_2SP;
            } else if (transform instanceof LambertAzimuthalEqualArea) {
                return LAMBERT_AZIMUTHAL_EQUAL_AREA;
            } else if (transform instanceof Orthographic) {
                return ORTHOGRAPHIC;
            } else if (transform instanceof PolarStereographic) {
                return POLAR_STEREOGRAPHIC;
            } else if (transform instanceof Stereographic) {
                return STEREOGRAPHIC;
            }
            //TODO ADD MORE
        }
        // Fallback on SPATIAL_REF to deal with projection which
        // doesn't have a CF Mapping. 
        return SPATIAL_REF;
    }

    public abstract NetCDFCoordinate[] getCoordinates();

    /** 
     * Return a {@link NetCDFProjection} instance for this 
     * specific CRS type. Note that WGS84 CRS and SPATIAL_REF 
     * won't return a projection. 
     */
    public abstract NetCDFProjection getNetCDFProjection();

    /** 
     * Contains basic information related to a NetCDF Coordinate such as:
     *  - short name (as an instance: x) 
     *  - long name (as an instance: x coordinate of projection)
     *  - standard name (as an instance: projection_x_coordinate)
     *  - the name of the associated dimension (as an instance: x)
     *  - the unit of measure of that coordinate (as an instance: m)
     * */
    public static class NetCDFCoordinate {

        private final static NetCDFCoordinate LAT_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.LAT, NetCDFUtilities.LATITUDE, NetCDFUtilities.LATITUDE,
                NetCDFUtilities.LAT, NetCDFUtilities.LAT_UNITS);

        private final static NetCDFCoordinate LON_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.LON, NetCDFUtilities.LONGITUDE, NetCDFUtilities.LONGITUDE,
                NetCDFUtilities.LON, NetCDFUtilities.LON_UNITS);

        private final static NetCDFCoordinate X_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.X, NetCDFUtilities.X_COORD_PROJ, NetCDFUtilities.X_PROJ_COORD,
                NetCDFUtilities.X, NetCDFUtilities.M);

        private final static NetCDFCoordinate Y_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.Y, NetCDFUtilities.Y_COORD_PROJ, NetCDFUtilities.Y_PROJ_COORD,
                NetCDFUtilities.Y, NetCDFUtilities.M);

        private final static NetCDFCoordinate[] LATLON_COORDS = new NetCDFCoordinate[] {
                LAT_COORDINATE, LON_COORDINATE };

        private final static NetCDFCoordinate[] YX_COORDS = new NetCDFCoordinate[] { Y_COORDINATE,
                X_COORDINATE };

        private String shortName;

        private String dimensionName;

        private String longName;

        private String units;

        private String standardName;

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public String getDimensionName() {
            return dimensionName;
        }

        public void setDimensionName(String dimensionName) {
            this.dimensionName = dimensionName;
        }

        public String getLongName() {
            return longName;
        }

        public void setName(String longName) {
            this.longName = longName;
        }

        public String getStandardName() {
            return standardName;
        }

        public void setStandardName(String standardName) {
            this.standardName = standardName;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        @Override
        public String toString() {
            return "NetCDFCoordinate [shortName=" + shortName + ", dimensionName=" + dimensionName
                    + ", longName=" + longName + ", units=" + units + ", standardName=" + standardName
                    + "]";
        }

        public NetCDFCoordinate(String shortName, String longName, String standardName,
                String dimensionName, String units) {
            this.shortName = shortName;
            this.longName = longName;
            this.standardName = standardName;
            this.dimensionName = dimensionName;
            this.units = units;
        }

    }

};
