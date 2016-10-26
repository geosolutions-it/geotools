package org.geotools.data.mongodb.complex;


import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MongoComplexFeatureType implements FeatureType {

    @Override
    public boolean isIdentified() {
        return false;
    }

    @Override
    public AttributeType getSuper() {
        return null;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public List<Filter> getRestrictions() {
        return null;
    }

    @Override
    public InternationalString getDescription() {
        return null;
    }

    @Override
    public Map<Object, Object> getUserData() {
        return null;
    }

    @Override
    public GeometryDescriptor getGeometryDescriptor() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        try {
            return CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Name getName() {
        return null;
    }

    @Override
    public Class<Collection<Property>> getBinding() {
        return null;
    }

    @Override
    public Collection<PropertyDescriptor> getDescriptors() {
        return null;
    }

    @Override
    public PropertyDescriptor getDescriptor(Name name) {
        return null;
    }

    @Override
    public PropertyDescriptor getDescriptor(String name) {
        return null;
    }

    @Override
    public boolean isInline() {
        return false;
    }
}
