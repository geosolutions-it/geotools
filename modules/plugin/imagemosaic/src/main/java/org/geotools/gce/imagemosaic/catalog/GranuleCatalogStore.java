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

import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.resources.coverage.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * A {@link GranuleStore} implementation wrapping a {@link GranuleCatalog}.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class GranuleCatalogStore extends GranuleCatalogSource implements GranuleStore {
    
    private final static FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;
    
    private Transaction transaction;
    
//    private Filter filterName;

    public GranuleCatalogStore(GranuleCatalog catalog, final String typeName) {
        super(catalog, typeName);
        //TODO: once we allow to create different catalogs (based on different featureTypes) 
        // we can stop filtering by name 
//        List<Filter> filters = new ArrayList<Filter>(); 
//        filters.add(FF.equal(FF.property("coverage"),
//                FF.literal(coverageName), true));
//        filterName = FF.and(filters);
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
        throw new UnsupportedOperationException("Operation not supported");
        
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
