/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic.catalog;

import java.io.IOException;
import java.util.Collection;

import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.gce.imagemosaic.GranuleDescriptor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.resources.coverage.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * A {@link GranuleStore} implementation wrapping a {@link GranuleCatalog}.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class GranuleCatalogStore implements GranuleStore {
    
    private final static FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;
    
    /** The underlying {@link GranuleCatalog} */
    private GranuleCatalog catalog;

    private Transaction transaction;
    
    private String typeName;
    
//    private Filter filterName;

    public GranuleCatalogStore(GranuleCatalog catalog, final String typeName) {
        super();
        //TODO: once we allow to create different catalogs (based on different featureTypes) 
        // we can stop filtering by name 
        this.catalog = catalog;
        this.typeName = typeName;
//        List<Filter> filters = new ArrayList<Filter>(); 
//        filters.add(FF.equal(FF.property("coverage"),
//                FF.literal(coverageName), true));
//        filterName = FF.and(filters);
    }
    
    @Override
    public SimpleFeatureCollection getGranules(Query q) throws IOException {
        q.setTypeName(typeName);
        // Filtering by coverageName
//        Filter filter = q.getFilter();
//        if (filter != null) {
//            filter = FF.and(filter, filterName);
//        } else {
//            filter = filterName;
//        }
//        q.setFilter(filter);
        
        // TODO: Optimize me
        Collection<GranuleDescriptor> granules = catalog.getGranules(q);
        SimpleFeatureCollection collection = new ListFeatureCollection(catalog.getType(typeName));
        for (GranuleDescriptor granule: granules) {
            ((ListFeatureCollection)collection).add(granule.getOriginator());
        }
        return collection;
    }

    @Override
    public int getCount(Query q) throws IOException {
        // TODO Optmize this call
        return getGranules(q).size();
    }

    @Override
    public ReferencedEnvelope getBounds(Query q) throws IOException {
        // TODO Optimize this call
        return getGranules(q).getBounds();
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return catalog.getType(typeName);
    }

    @Override
    public void dispose() throws IOException {
        // TODO: check if we need to dispose it or not
        // Does nothing, the catalog should be disposed by the user
        
    }

    @Override
    public void addGranules(SimpleFeatureCollection granules) {
        checkTransaction();
        SimpleFeatureIterator features = granules.features();
        boolean firstSchemaCompatibilityCheck = false;
        while (features.hasNext()) {
            SimpleFeature feature = features.next();
            if (!firstSchemaCompatibilityCheck) {
                firstSchemaCompatibilityCheck = true;
                checkSchemaCompatibility(feature);
            }
            try {
                catalog.addGranule(typeName, feature, transaction);
            } catch (IOException e) {
                throw new RuntimeException("Exception occurred while adding granules to the catalog", e);
            }
        }
    }

    private void checkTransaction() {
        if (transaction == null) {
            throw new IllegalArgumentException("No transaction available for this store");
        }
        
    }

    /**
     * Check whether the specified feature has the same schema of the catalog where we are adding that feature.
     * 
     * @param feature a sample SimpleFeature for compatibility check
     */
    private void checkSchemaCompatibility(final SimpleFeature feature) {
        try {
            if (!feature.getType().equals(catalog.getType(typeName))) {
                throw new IllegalArgumentException("The schema of the provided collection is not the same of the underlying catalog");
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception occurred while getting the underlying catalog schema");
        }
        
    }

    @Override
    public int removeGranules(Filter filter) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateGranules(String[] attributeNames, Object[] attributeValues, Filter filter) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public void setTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        this.transaction = transaction;
    }

}
