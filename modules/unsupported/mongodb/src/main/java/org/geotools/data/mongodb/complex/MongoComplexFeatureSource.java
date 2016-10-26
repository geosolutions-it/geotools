package org.geotools.data.mongodb.complex;


import com.mongodb.DBCollection;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class MongoComplexFeatureSource implements FeatureSource<MongoComplexFeatureType, MongoComplexFeature> {

    private final DBCollection collection;

    public MongoComplexFeatureSource(DBCollection collection) {
        this.collection = collection;
    }

    @Override
    public Name getName() {
        return new NameImpl("datex");
    }

    @Override
    public ResourceInfo getInfo() {
        return null;
    }

    @Override
    public DataAccess<MongoComplexFeatureType, MongoComplexFeature> getDataStore() {
        return null;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return null;
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
    }

    @Override
    public FeatureCollection<MongoComplexFeatureType, MongoComplexFeature> getFeatures(Filter filter) throws IOException {
        return null;
    }

    @Override
    public FeatureCollection<MongoComplexFeatureType, MongoComplexFeature> getFeatures(Query query) throws IOException {
        query.
        return new MongoComplexFeaturesCollection(collection);
    }

    @Override
    public FeatureCollection<MongoComplexFeatureType, MongoComplexFeature> getFeatures() throws IOException {
        return null;
    }

    @Override
    public MongoComplexFeatureType getSchema() {
        return new MongoComplexFeatureType();
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return null;
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return null;
    }

    @Override
    public int getCount(Query query) throws IOException {
        return 0;
    }

    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return null;
    }
}
