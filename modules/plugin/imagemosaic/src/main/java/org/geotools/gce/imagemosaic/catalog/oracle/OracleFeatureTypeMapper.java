package org.geotools.gce.imagemosaic.catalog.oracle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.transform.Definition;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * An Oracle specific {@link FeatureTypeMapper} instance
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class OracleFeatureTypeMapper implements FeatureTypeMapper {
    
    private Name originalName;

    private String mappedName;

    private CoordinateReferenceSystem coordinateReferenceSystem;

    private List<Definition> definitions;

    private SimpleFeatureType wrappedFeatureType;

    private SimpleFeatureType innerFeatureType;

    private Map<Name, Definition> definitionsMapping;

    private SimpleFeatureSource simpleFeatureSource;

    @Override
    public Name getName() {
        return originalName;
    }

    @Override
    public String getMappedName() {
        return mappedName;
    }

    @Override
    public List<Definition> getDefinitions() {
        return definitions;
    }

    @Override
    public SimpleFeatureType getInnerFeatureType() {
        return innerFeatureType;
    }

    @Override
    public SimpleFeatureType getWrappedFeatureType() {
        return wrappedFeatureType;
    }
    
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }
    
    public OracleFeatureTypeMapper(SimpleFeatureType featureType) throws CQLException {
        wrappedFeatureType = featureType;
        originalName = featureType.getName();
        mappedName = originalName.getLocalPart();
        mappedName = mapName(mappedName);
        List<AttributeDescriptor> attributes = featureType.getAttributeDescriptors();
        definitions = new LinkedList<Definition>();
        definitionsMapping = new HashMap<Name, Definition>();
        for (AttributeDescriptor attribute : attributes) {
            final String originalAttribute = attribute.getLocalName();
            AttributeType type = attribute.getType();
            Class<?> binding = type.getBinding();
            String attributeName = mapName(originalAttribute);
            Definition definition = new Definition(originalAttribute, ECQL.toExpression(attributeName), binding);
            definitions.add(definition);
            definitionsMapping.put(attribute.getName(), definition);
        }
        createOracleFeatureType();
    }

    private void createOracleFeatureType() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(mappedName);
        List<AttributeDescriptor> descriptors = wrappedFeatureType.getAttributeDescriptors();
//        for (Definition definition: definitions) {
//            AttributeDescriptor attrib = definition.getAttributeDescriptor(wrappedFeatureType);
//            tb.add(attrib);
//        }
        for (AttributeDescriptor descriptor : descriptors) {
            Name name = descriptor.getName();
            Definition definition = definitionsMapping.get(name);
            AttributeType type = descriptor.getType();
            if (type instanceof GeometryType) {
                coordinateReferenceSystem = ((GeometryType) type).getCoordinateReferenceSystem();
                tb.add(definition.getExpression().toString(), definition.getBinding(), coordinateReferenceSystem);
            } else {
                
                tb.add(definition.getExpression().toString(), definition.getBinding());
            }
        }
        innerFeatureType = tb.buildFeatureType();
    }

    @Override
    public SimpleFeatureSource getSimpleFeatureSource() {
        return simpleFeatureSource;
    }

    public void setSimpleFeatureSource(SimpleFeatureSource simpleFeatureSource) {
        this.simpleFeatureSource = simpleFeatureSource;
    }
    
    static String mapName(String name) {
        String mappedName = name.toUpperCase();
        mappedName = mappedName.length() > 30 ? mappedName.substring(0, 30) : mappedName;
        return mappedName;
    }

}
