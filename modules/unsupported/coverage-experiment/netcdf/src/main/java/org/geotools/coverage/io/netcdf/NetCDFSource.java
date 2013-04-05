/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
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

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.coverage.io.CoverageReadRequest;
import org.geotools.coverage.io.CoverageResponse;
import org.geotools.coverage.io.impl.DefaultCoverageSource;
import org.geotools.imageio.netcdf.NetCDFImageReader;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;
/**
 * Implementation of a coverage source for netcdf data
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 * @source $URL$
 */
public class NetCDFSource extends DefaultCoverageSource {

    /** Logger. */
    private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(NetCDFSource.class.toString());

    NetCDFImageReader reader;

    public NetCDFSource(final NetCDFImageReader reader, final Name name ) {
        super(name, reader.createCoverageDescriptor(name));
        this.reader = reader;
    }

    @Override
    public CoverageResponse read(CoverageReadRequest request, ProgressListener listener)
            throws IOException {
        ensureNotDisposed();

        NetCDFCoverageReadRequest coverageRequest = new NetCDFCoverageReadRequest(this, request);
        NetCDFResponse netCDFresponse = new NetCDFResponse(coverageRequest);
        return netCDFresponse.createResponse();
    }
}
