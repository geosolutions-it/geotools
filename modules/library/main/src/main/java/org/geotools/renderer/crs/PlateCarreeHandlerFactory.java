/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2024, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.renderer.crs;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.projection.EquidistantCylindrical;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.geotools.referencing.operation.projection.PlateCarree;

/** Returns a {@link ProjectionHandler} for the {@link PlateCarree} projection */
public class PlateCarreeHandlerFactory implements ProjectionHandlerFactory {

    private static final ReferencedEnvelope VALID_AREA =
            new ReferencedEnvelope(
                    -Integer.MAX_VALUE, Integer.MAX_VALUE, -90, 90, DefaultGeographicCRS.WGS84);

    @Override
    public ProjectionHandler getHandler(
            ReferencedEnvelope renderingEnvelope,
            CoordinateReferenceSystem sourceCrs,
            boolean wrap,
            int maxWraps)
            throws FactoryException {

        if (renderingEnvelope == null) return null;
        MapProjection mapProjection =
                CRS.getMapProjection(renderingEnvelope.getCoordinateReferenceSystem());
        boolean isEquidistantCylindrical = mapProjection instanceof EquidistantCylindrical;
        if (!(isEquidistantCylindrical)) return null;

        ProjectionHandler handler;
        double centralMeridian =
                mapProjection
                        .getParameterValues()
                        .parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode())
                        .doubleValue();
        if (wrap && maxWraps > 0) {
            handler =
                    new WrappingProjectionHandler(
                            renderingEnvelope, VALID_AREA, sourceCrs, centralMeridian, maxWraps);
        } else {
            handler = new ProjectionHandler(sourceCrs, VALID_AREA, renderingEnvelope);
            handler.setCentralMeridian(centralMeridian);
        }
        if (!wrap) {
            // for this projection, if wrapping is not enabled, do not query across the
            // dateline
            handler.queryAcrossDateline = false;
        }
        return handler;
    }
}
