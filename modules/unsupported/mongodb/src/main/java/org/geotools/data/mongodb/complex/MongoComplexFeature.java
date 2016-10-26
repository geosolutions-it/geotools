package org.geotools.data.mongodb.complex;

import com.mongodb.DBObject;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

import java.util.Collection;
import java.util.Map;

public class MongoComplexFeature implements Feature {

    private final DBObject mongoObject;

    public DBObject getMongoObject() {
        return mongoObject;
    }

    public MongoComplexFeature(DBObject mongoObject) {
        this.mongoObject = mongoObject;
    }

    @Override
    public AttributeDescriptor getDescriptor() {
        return null;
    }

    @Override
    public Name getName() {
        return null;
    }

    @Override
    public boolean isNillable() {
        return false;
    }

    @Override
    public Map<Object, Object> getUserData() {
        return null;
    }

    @Override
    public FeatureType getType() {
        return null;
    }

    @Override
    public void setValue(Collection<Property> values) {
    }

    @Override
    public Collection<? extends Property> getValue() {
        return null;
    }

    @Override
    public void setValue(Object newValue) {
    }

    @Override
    public Collection<Property> getProperties(Name name) {
        return null;
    }

    @Override
    public Property getProperty(Name name) {
        return null;
    }

    @Override
    public Collection<Property> getProperties(String name) {
        return null;
    }

    @Override
    public Collection<Property> getProperties() {
        return null;
    }

    @Override
    public Property getProperty(String name) {
        return null;
    }

    @Override
    public void validate() throws IllegalAttributeException {
    }

    @Override
    public FeatureId getIdentifier() {
        return null;
    }

    @Override
    public BoundingBox getBounds() {
        return null;
    }

    @Override
    public GeometryAttribute getDefaultGeometryProperty() {
        return null;
    }

    @Override
    public void setDefaultGeometryProperty(GeometryAttribute geometryAttribute) {
    }
}
