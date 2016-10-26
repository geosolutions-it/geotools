package org.geotools.data.mongodb.complex;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.FeatureIteratorImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MongoComplexFeaturesCollection implements FeatureCollection<MongoComplexFeatureType, MongoComplexFeature> {

    private final DBCollection collection;

    public MongoComplexFeaturesCollection(DBCollection collection) {
        this.collection = collection;
    }

    @Override
    public FeatureIterator<MongoComplexFeature> features() {
        List<MongoComplexFeature> features = new ArrayList<>();
        DBCursor cursor = collection.find();
        while (cursor.hasNext()) {
            features.add(new MongoComplexFeature(cursor.next()));
        }
        return new FeatureIteratorImpl(features);
    }

    @Override
    public MongoComplexFeatureType getSchema() {
        return new MongoComplexFeatureType();
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {

    }

    @Override
    public FeatureCollection<MongoComplexFeatureType, MongoComplexFeature> subCollection(Filter filter) {
        return null;
    }

    @Override
    public FeatureCollection<MongoComplexFeatureType, MongoComplexFeature> sort(SortBy order) {
        return null;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return null;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <O> O[] toArray(O[] a) {
        return null;
    }
}
