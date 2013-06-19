package org.geotools.gce.imagemosaic.catalog.oracle;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.transform.Definition;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Specific Oracle implementation for a {@link DataStoreWrapper}
 * Oracle DB has a couple of limitations:
 * 1) All attributes and type names are UPPERCASE
 * 2) attribute and type names can't be longer than 30 chars
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class OracleDatastoreWrapper extends DataStoreWrapper {

static final String NAME = "NAME";
    
    static final String MAPPEDNAME = "MAPPEDNAME";
    
    static final String SCHEMA = "SCHEMA";
    
    static final String COORDINATE_REFERENCE_SYSTEM = "CRS";
    
    public OracleDatastoreWrapper(DataStore datastore, String location) {
        super(datastore, location + File.separatorChar + ".orcl");
    }

    @Override
    protected FeatureTypeMapper getFeatureTypeMapper(SimpleFeatureType featureType) throws Exception {
        return new OracleFeatureTypeMapper(featureType);
    }
    
    @Override
    protected FeatureTypeMapper getFeatureTypeMapper(final Properties props) throws Exception {
        
        SimpleFeatureType indexSchema;
        try {
            indexSchema = DataUtilities.createType(props.getProperty(NAME), props.getProperty(SCHEMA));
            CoordinateReferenceSystem crs = CRS.parseWKT(props.getProperty(COORDINATE_REFERENCE_SYSTEM));
            indexSchema = DataUtilities.createSubType(indexSchema,
                    DataUtilities.attributeNames(indexSchema), crs);
        } catch (Throwable e) {
//            if (LOGGER.isLoggable(Level.FINE))
//                LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            indexSchema = null;
        }
            return getFeatureTypeMapper(indexSchema);
    }


    @Override
    protected SimpleFeatureSource transformFeatureStore(SimpleFeatureStore store,
            FeatureTypeMapper mapper) throws IOException {
        SimpleFeatureSource transformedSource = mapper.getSimpleFeatureSource();
        if (transformedSource != null) {
            return transformedSource;
        } else {
            transformedSource = (SimpleFeatureSource) new OracleTransformFeatureStore(store, mapper.getName(), mapper.getDefinitions(), datastore);
            ((OracleFeatureTypeMapper)mapper).setSimpleFeatureSource(transformedSource);
            return transformedSource;
        }
    }

    @Override
    protected void storeMapper(FeatureTypeMapper mapper) {
            Properties properties = new Properties();
            String typeName = mapper.getName().toString();
            properties.setProperty(NAME, typeName);
            properties.setProperty(MAPPEDNAME, mapper.getMappedName().toString());
            List<Definition> definitions = mapper.getDefinitions();
            StringBuilder builder = new StringBuilder();
            for (Definition definition: definitions) {
                builder.append(definition.getName()).append(":").append(definition.getBinding().getName()).append(",");
            }
            String schema = builder.toString();
            schema = schema.substring(0, schema.length() - 1 );
            properties.setProperty(SCHEMA, schema);
            properties.setProperty(COORDINATE_REFERENCE_SYSTEM, mapper.getCoordinateReferenceSystem().toWKT());
            storeProperties(properties, typeName);
        
    }
}
