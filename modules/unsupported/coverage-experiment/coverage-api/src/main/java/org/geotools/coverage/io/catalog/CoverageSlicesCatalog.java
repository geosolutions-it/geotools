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
package org.geotools.coverage.io.catalog;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.Transaction;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.data.h2.H2JNDIDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.Utilities;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;

/**
 * This class simply builds an index for fast indexed queries.
 * 
 * TODO: we may consider converting {@link CoverageSlice}s to {@link SimpleFeature}s
 */
public class CoverageSlicesCatalog {

    public static final String SCAN_FOR_TYPENAMES = "ScanTypeNames";
    /** Logger. */
    final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(CoverageSlicesCatalog.class);

    final static FilterFactory2 FILTER_FACTORY = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    /** The slices index store */
    private DataStore slicesIndexStore;

    /** The feature type name */
    private Set<String> typeNames = new HashSet<String>();
//    private String typeName;

    private String geometryPropertyName;

    private ReferencedEnvelope bounds;

    private String parentLocation;
    
    public final static String IMAGE_INDEX_ATTR = "imageindex";

    private final SoftValueHashMap<Integer, CoverageSlice> CoverageSliceDescriptorsCache = new SoftValueHashMap<Integer, CoverageSlice>(0);

    public CoverageSlicesCatalog(final Map<String, Serializable> params, final boolean create,
            final DataStoreFactorySpi spi) {
        Utilities.ensureNonNull("params", params);
        Utilities.ensureNonNull("spi", spi);
        try {

            this.parentLocation = (String) params.get("ParentLocation");

            // H2 workadound
            if (spi instanceof H2DataStoreFactory || spi instanceof H2JNDIDataStoreFactory) {
                if (params.containsKey(H2DataStoreFactory.DATABASE.key)) {
                    String dbname = (String) params.get(H2DataStoreFactory.DATABASE.key);
                    // H2 database URLs must not be percent-encoded: see GEOT-4262.
                    params.put(H2DataStoreFactory.DATABASE.key,
                            "file:"  + (new File(DataUtilities.urlToFile(new URL(parentLocation)),
                                            dbname)).getPath());
                }
            }

            // creating a store, this might imply creating it for an existing underlying store or
            // creating a brand new one
            if (!create){
                slicesIndexStore = spi.createDataStore(params);
            } else {
                // this works only with the shapefile datastore, not with the others
                // therefore I try to catch the error to try and use themethdo without *New*
                try {
                    slicesIndexStore = spi.createNewDataStore(params);
                } catch (UnsupportedOperationException e) {
                    slicesIndexStore = spi.createDataStore(params);
                }
            }

            // is this a new store? If so we do not set any properties
            if (create) {
                return;
            }

            // if this is not a new store let's extract basic properties from it
            String typeName = null;
            boolean scanForTypeNames = false;
            
            // Handle multiple typeNames
            if(params.containsKey("TypeName")){
                typeName=(String) params.get("TypeName");
            }  
            if (params.containsKey(SCAN_FOR_TYPENAMES)) {
                scanForTypeNames = (Boolean) params.get(SCAN_FOR_TYPENAMES);
            }
            
            if (scanForTypeNames) {
                String[] typeNames = slicesIndexStore.getTypeNames();
                if (typeNames != null) {
                    for (String tn : typeNames) {
                        this.typeNames.add(tn);
                    }
                }
            } else if (typeName != null) {
                addTypeName(typeName, false);
            }
                // Oracle trick
//                if (spi instanceof OracleNGOCIDataStoreFactory
//                        || spi instanceof OracleNGJNDIDataStoreFactory
//                        || spi instanceof OracleNGDataStoreFactory) {
//                    this.typeName = typeName.toUpperCase();
//                    if (locationAttribute != null) {
//                        this.locationAttribute = this.locationAttribute.toUpperCase();
//                    }
//                }
//            }
            extractBasicProperties(typeName);
        } catch (Throwable e) {
            try {
                if (slicesIndexStore != null)
                    slicesIndexStore.dispose();
            } catch (Throwable e1) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e1.getLocalizedMessage(), e1);
            } finally {
                slicesIndexStore = null;
            }

            throw new IllegalArgumentException(e);
        }

    }

    /**
     * If the underlying store has been disposed we throw an {@link IllegalStateException}.
     * <p>
     * We need to arrive here with at least a read lock!
     * 
     * @throws IllegalStateException in case the underlying store has been disposed.
     */
    private void checkStore() throws IllegalStateException {
        if (slicesIndexStore == null) {
            throw new IllegalStateException("The index store has been disposed already.");
        }
    }

    private void extractBasicProperties(String typeName) throws IOException {

        if (typeName == null) {
            final String[] typeNames = slicesIndexStore.getTypeNames();
            if (typeNames == null || typeNames.length <= 0)
                throw new IllegalArgumentException(
                        "BBOXFilterExtractor::extractBasicProperties(): Problems when opening the index,"
                                + " no typenames for the schema are defined");

            if (typeName == null) {
                typeName = typeNames[0];
                addTypeName(typeName, false);
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.warning("BBOXFilterExtractor::extractBasicProperties(): passed typename is null, using: "
                            + typeName);
            }

            // loading all the features into memory to build an in-memory index.
            for (String type : typeNames) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.fine("BBOXFilterExtractor::extractBasicProperties(): Looking for type \'"
                            + typeName
                            + "\' in DataStore:getTypeNames(). Testing: \'"
                            + type
                            + "\'.");
                if (type.equalsIgnoreCase(typeName)) {
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine("BBOXFilterExtractor::extractBasicProperties(): SUCCESS -> type \'"
                                + typeName + "\' is equalsIgnoreCase() to \'" + type + "\'.");
                    typeName = type;
                    addTypeName(typeName, false);
                    break;
                }
            }
        }

        final SimpleFeatureSource featureSource = slicesIndexStore.getFeatureSource(typeName);
        if (featureSource != null){
//            bounds = featureSource.getBounds();
        } else {
            throw new IOException(
                    "BBOXFilterExtractor::extractBasicProperties(): unable to get a featureSource for the qualified name"
                            + typeName);
        }
        
        final FeatureType schema = featureSource.getSchema();
        if (schema != null) {
            geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("BBOXFilterExtractor::extractBasicProperties(): geometryPropertyName is set to \'"
                        + geometryPropertyName + "\'.");

        } else {
            throw new IOException(
                    "BBOXFilterExtractor::extractBasicProperties(): unable to get a schema from the featureSource");
        }

    }
    
    private void addTypeName(String typeName, final boolean check) {
          if (check && this.typeNames.contains(typeName)) {
              throw new IllegalArgumentException("This typeName already exists: " + typeName);
          }
          this.typeNames.add(typeName);    
  }
    
    public String[] getTypeNames() {
        if (this.typeNames != null && !this.typeNames.isEmpty()) {
            return (String[]) this.typeNames.toArray(new String[]{});
        }
        return null;
    }

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    /**
     * Finds the granules that intersects the provided {@link BoundingBox}:
     * 
     * @param envelope The {@link BoundingBox} to test for intersection.
     * @return Collection of {@link Feature} that intersect the provided {@link BoundingBox}.
     * @throws IOException
     */
    public List<CoverageSlice> getGranules(final String typeName, final BoundingBox envelope) throws IOException {
        Utilities.ensureNonNull("envelope", envelope);
        final Query q = new Query(typeName);
        Filter filter = FILTER_FACTORY.bbox(FILTER_FACTORY.property(geometryPropertyName),
                ReferencedEnvelope.reference(envelope));
        q.setFilter(filter);
        return getGranules(q);

    }

    /**
     * Finds the granules that intersects the provided {@link BoundingBox}:
     * 
     * @param envelope The {@link BoundingBox} to test for intersection.
     * @return List of {@link Feature} that intersect the provided {@link BoundingBox}.
     * @throws IOException
     */
    public void getGranules(final String typeName, final BoundingBox envelope, final GranuleCatalogVisitor visitor)
            throws IOException {
        Utilities.ensureNonNull("envelope", envelope);
        final Query q = new Query(typeName);
        Filter filter = FILTER_FACTORY.bbox(FILTER_FACTORY.property(geometryPropertyName),
                ReferencedEnvelope.reference(envelope));
        q.setFilter(filter);
        getGranules(q, visitor);

    }

    public void dispose() {
        final Lock l = rwLock.writeLock();
        try {
            l.lock();
            try {
                if (slicesIndexStore != null)
                    slicesIndexStore.dispose();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            } finally {
                slicesIndexStore = null;
            }
        } finally {
            l.unlock();
        }
    }

    public void addGranule(final String typeName, final SimpleFeature granule, final Transaction transaction)
            throws IOException {
        final DefaultFeatureCollection collection= new DefaultFeatureCollection();
        collection.add(granule);
        addGranules(typeName, collection, transaction);
    }

    public void addGranules(final String typeName, final SimpleFeatureCollection granules, final Transaction transaction)
            throws IOException {
        Utilities.ensureNonNull("granuleMetadata", granules);
        final Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            // check if the index has been cleared
            checkStore();
            

            final SimpleFeatureStore store = (SimpleFeatureStore) slicesIndexStore.getFeatureSource(typeName);
            store.setTransaction(transaction);
            store.addFeatures(granules);

            // update bounds
//            bounds = null;
        } finally {
            lock.unlock();
        }
    }

    public void getGranules(final Query q, final GranuleCatalogVisitor visitor) throws IOException {
        Utilities.ensureNonNull("query", q);

        final Lock lock = rwLock.readLock();
        try {
            lock.lock();
            checkStore();
            String typeName = q.getTypeName();
            
            //
            // Load tiles informations, especially the bounds, which will be reused
            //
            final SimpleFeatureSource featureSource = slicesIndexStore.getFeatureSource(typeName);
            if (featureSource == null) {
                throw new NullPointerException(
                        "The provided SimpleFeatureSource is null, it's impossible to create an index!");
            }
            final SimpleFeatureCollection features = featureSource.getFeatures(q);
            if (features == null)
                throw new NullPointerException(
                        "The provided SimpleFeatureCollection is null, it's impossible to create an index!");

            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("Index Loaded");

            // load the feature from the underlying datastore as needed
            final SimpleFeatureIterator it = features.features();
            try {
                if (!it.hasNext()) {
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine("The provided SimpleFeatureCollection  or empty, it's impossible to create an index!");
                    return;
                }

                // getting the features
                while (it.hasNext()) {
                    SimpleFeature feature = it.next();
                    final SimpleFeature sf = (SimpleFeature) feature;
                    final CoverageSlice granule;

                    // caching by granule's index
                    synchronized (CoverageSliceDescriptorsCache) {
                        Integer granuleIndex = (Integer) sf.getAttribute(IMAGE_INDEX_ATTR);
                        if (CoverageSliceDescriptorsCache.containsKey(granuleIndex)) {
                            granule = CoverageSliceDescriptorsCache.get(granuleIndex);
                        } else {
                            // create the granule coverageDescriptor
                            granule = new CoverageSlice(sf);
                            CoverageSliceDescriptorsCache.put(granuleIndex, granule);
                        }
                    }
                    visitor.visit(granule, null);
                }
            } finally {
                it.close();
            }
        } catch (Throwable e) {
            final IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        } finally {
            lock.unlock();

        }
    }

    public List<CoverageSlice> getGranules(final Query q) throws IOException {
        // create a list to return and reuse the visitor enabled method
        //TODO revisit this to deal with iterators
        final List<CoverageSlice> returnValue = new ArrayList<CoverageSlice>();
        getGranules(q, new GranuleCatalogVisitor() {
            public void visit(CoverageSlice granule, Object o) {
                returnValue.add(granule);
            }
        });
        return returnValue;
    }

    public Collection<CoverageSlice> getGranules(final String typeName) throws IOException {
        return getGranules(typeName, getBounds(typeName));
    }

    public ReferencedEnvelope getBounds(final String typeName) {
        final Lock lock = rwLock.readLock();
        try {
            lock.lock();
            checkStore();
            if (bounds == null) {
                bounds = this.slicesIndexStore.getFeatureSource(typeName).getBounds();
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            bounds = null;
        } finally {
            lock.unlock();
        }

        return bounds;
    }

    public void createType(String namespace, String typeName, String typeSpec) throws IOException,
            SchemaException {
        Utilities.ensureNonNull("typeName", typeName);
        Utilities.ensureNonNull("typeSpec", typeSpec);
        final Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            checkStore();

            final SimpleFeatureType featureType = DataUtilities.createType(namespace, typeName, typeSpec);
            slicesIndexStore.createSchema(featureType);
            // Oracle trick
//            if (spi instanceof OracleNGOCIDataStoreFactory
//                    || spi instanceof OracleNGJNDIDataStoreFactory
//                    || spi instanceof OracleNGDataStoreFactory) {
//                this.typeName = this.typeName.toUpperCase();
//                if (locationAttribute != null) {
//                    this.locationAttribute = this.locationAttribute.toUpperCase();
//                }
//            }
            extractBasicProperties(typeName);
        } finally {
            lock.unlock();
        }

    }

    public void createType(SimpleFeatureType featureType) throws IOException {
        Utilities.ensureNonNull("featureType", featureType);
        final Lock lock = rwLock.writeLock();
        String typeName = null;
        try {
            lock.lock();
            checkStore();

            slicesIndexStore.createSchema(featureType);
            typeName = featureType.getTypeName();
            // Oracle trick
//            if (spi instanceof OracleNGOCIDataStoreFactory
//                    || spi instanceof OracleNGJNDIDataStoreFactory
//                    || spi instanceof OracleNGDataStoreFactory) {
//                this.typeName = this.typeName.toUpperCase();
//                if (locationAttribute != null) {
//                    this.locationAttribute = this.locationAttribute.toUpperCase();
//                }
//            }
            if (typeName != null) {
                addTypeName(typeName, true);
            }
            extractBasicProperties(typeName);
        } finally {
            lock.unlock();
        }

    }

    public void createType(String identification, String typeSpec) throws SchemaException,
            IOException {
        Utilities.ensureNonNull("typeSpec", typeSpec);
        Utilities.ensureNonNull("identification", identification);
        final Lock lock = rwLock.writeLock();
        String typeName = null;
        try {
            lock.lock();
            checkStore();
            final SimpleFeatureType featureType = DataUtilities.createType(identification, typeSpec);
            slicesIndexStore.createSchema(featureType);
            typeName = featureType.getTypeName();
            // Oracle trick
//            if (spi instanceof OracleNGOCIDataStoreFactory
//                    || spi instanceof OracleNGJNDIDataStoreFactory
//                    || spi instanceof OracleNGDataStoreFactory) {
//                this.typeName = this.typeName.toUpperCase();
//                if (locationAttribute != null) {
//                    this.locationAttribute = this.locationAttribute.toUpperCase();
//                }
//            }
            extractBasicProperties(typeName);
        } finally {
            lock.unlock();
        }

    }

    public SimpleFeatureType getSchema(final String typeName) throws IOException {
        final Lock lock = rwLock.readLock();
        try {
            lock.lock();
            checkStore();
            if (typeName == null) {
                return null;
            }
            return slicesIndexStore.getSchema(typeName);
        } finally {
            lock.unlock();
        }

    }

    public void computeAggregateFunction(Query query, FeatureCalc function) throws IOException {
        final Lock lock = rwLock.readLock();
        try {
            lock.lock();
            checkStore();
            SimpleFeatureSource fs = slicesIndexStore.getFeatureSource(query.getTypeName());

            if (fs instanceof ContentFeatureSource)
                ((ContentFeatureSource) fs).accepts(query, function, null);
            else {
                final SimpleFeatureCollection collection = fs.getFeatures(query);
                collection.accepts(function, null);

            }
        } finally {
            lock.unlock();
        }

    }

    public QueryCapabilities getQueryCapabilities(final String typeName) {
        final Lock lock = rwLock.readLock();
        try {
            lock.lock();
            checkStore();

            return slicesIndexStore.getFeatureSource(typeName).getQueryCapabilities();
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.log(Level.INFO, "Unable to collect QueryCapabilities", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        // warn people
        if (this.slicesIndexStore != null) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("This granule catalog was not properly dispose as it still points to:"
                        + slicesIndexStore.getInfo().toString());
            }
            // try to dispose the underlying store if it has not been disposed yet
            this.dispose();
        }

    }

    public int removeGranules(Query query) {
        throw new UnsupportedOperationException("This Catalog does not support removing granules");
    }
}
