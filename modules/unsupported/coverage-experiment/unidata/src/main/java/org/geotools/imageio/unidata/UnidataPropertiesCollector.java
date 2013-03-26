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
package org.geotools.imageio.unidata;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.coverage.io.CoverageSourceDescriptor;
import org.geotools.coverage.io.catalog.CoverageSlice;
import org.geotools.coverage.io.catalog.CoverageSlicesCatalog;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.gce.imagemosaic.CoveragesManager;
import org.geotools.gce.imagemosaic.CoveragesManager.RasterManager;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.resources.coverage.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;

/**
 * A Properties collector implementation based on Unidata classes which get access to the 
 * underlying dataset and put all the collected information within the catalog index.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class UnidataPropertiesCollector extends CatalogPropertiesCollector {

    UnidataImageReaderSpi spi = null;

    private final static FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;
    
    URL url; 
    
    private CoveragesManager coveragesManager = null;

    /**
     * Collect information from the provided url; setup feature collections on top of it and put them
     * on the underlying granuleCatalog.
     * @param url
     * @throws IOException
     */
    public void setCatalog(GranuleCatalog index) throws IOException {
        

        UnidataImageReader reader = (UnidataImageReader) spi.createReaderInstance();
        
        //TODO FINALIZE-CLOSE THAT.
        reader.setInput(url);
        List<Name> names = reader.getNames();

        boolean goodCandidate = checkCompliance(names);
        if (goodCandidate) {

            Transaction transaction = new DefaultTransaction("updatingIndexTransaction"
                    + System.nanoTime());
            boolean rollback = false;
            try {
                CoverageSlicesCatalog catalog = reader.getCatalog();
                
                // TODO: NEED TO RETRIEVE attributes from SCHEMA
                String location = null; // Build location from URL.
                String locationAttribute = null; // NEED TO FIX THEM
                String timeAttribute = null;
                String elevationAttribute = null;

                for (Name name : names) {
                    CatalogSchema catalogSchema = indexer.schemaMapping.get(name);
                    if (catalogSchema == null) {
                        throw new IllegalArgumentException("unsupported coverage name: " + name);
                    }

                    SimpleFeatureType schema = catalogSchema.schema;
                    RasterManager manager = coveragesManager.getRasterManager(name.toString());
                    final CoverageSourceDescriptor descriptor = reader.getCoverageDescriptor(name);
                    if (manager == null) {
                        
                        // No rasterManager is currently existing for that coverage 
                        // Need to create a new one.
//                        reader.getc
                        
                    }
                    // 
                    // Scan along the available coverages
                    //
                    
                    final DefaultFeatureCollection collection = new DefaultFeatureCollection("df",
                            index.getType());
                    // Get coverage domain
                    final BoundingBox box = descriptor.getSpatialDomain().getSpatialElements(true, null)
                            .iterator().next();
                    
                    boolean hasTemporalDomain = descriptor.isHasTemporalDomain();
                    boolean hasVerticalDomain = descriptor.isHasVerticalDomain();
                    
                    // Setup a filter on coverageName
                    final Query query = createQueryForName(name);
                    Collection<CoverageSlice> slices = catalog.getGranules(query);

                    for (CoverageSlice slice : slices) {
                        
                        // get the feature associated to the unidata slice
                        final SimpleFeature originatingFeature = slice.getOriginator();
                        
                        final SimpleFeature feature = DataUtilities.template(schema);
                        feature.setAttribute(schema.getGeometryDescriptor().getLocalName(), box);
                        feature.setAttribute(locationAttribute, location);

                        // TODO: Need to set other attributes (time, elevation, ...)
                        if (hasTemporalDomain) {
                            // What about converting input time to ranges
                            feature.setAttribute(timeAttribute, originatingFeature.getAttribute(CoverageSlice.Attributes.TIME));
                        }
                        if (hasVerticalDomain) {
                            // What about converting input value to ranges
                            feature.setAttribute(elevationAttribute, originatingFeature.getAttribute(CoverageSlice.Attributes.ELEVATION));
                        }
                        
                        // 
                        // what about setting runtime/modify time
                        collection.add(feature);
                    }
                    index.addGranules(collection, transaction);
                }

            } catch (Throwable e) {
                rollback = true;
                throw new IOException(e);
            } finally {
                if (rollback) {
                    transaction.rollback();
                } else {
                    transaction.commit();
                }
                try {
                    transaction.close();
                } catch (Throwable t) {

                }
            }

        }
    }

    private Query createQueryForName(Name name) {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(FF.equal(FF.property(CoverageSlice.Attributes.COVERAGENAME),
                FF.literal(name.toString()), true));
        Filter filter = FF.and(filters);
        Query query = new Query();
        query.setFilter(filter);
        return query;
    }

    /** 
     * Check whether the names contained on that dataset are supported by the dictionary
     * Also check if they are conformal with the rules
     * @param names
     * @param schema
     * @return
     */
    private boolean checkCompliance(List<Name> names) {
        // TODO Auto-generated method stub
        return false;
    }

}
