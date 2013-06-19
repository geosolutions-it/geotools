package org.geotools.gce.imagemosaic.catalog.oracle;

import java.io.IOException;
import java.util.List;

import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.transform.Definition;
import org.geotools.data.transform.TransformFeatureStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

public class OracleTransformFeatureStore extends TransformFeatureStore{

    DataStore datastore;
    
    public OracleTransformFeatureStore(SimpleFeatureStore store, Name name,
            List<Definition> definitions, DataStore datastore) throws IOException {
        super(store, name, definitions);
        this.datastore = datastore;
    }

    @Override
    public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
        return datastore;
    }

    
}
