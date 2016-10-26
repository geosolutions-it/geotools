package org.geotools.data.mongodb.complex;

import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;

import java.awt.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class MongoComplexDataAccessFactory implements DataAccessFactory {

    public static final Param NAMESPACE = new Param("namespace", String.class, "Namespace prefix", false);
    public static final Param DATA_STORE_URI = new Param("data_store", String.class, "MongoDB URI", true, "mongodb://localhost/<database name>");
    public static final Param SCHEMA_STORE_URI = new Param("schema_store", String.class, "Schema Store URI", true, "file://<absolute path>");
    public static final Param DATA_STORE_TYPE = new Param("data_store_type", String.class, "Data Store Type", true, "simple");

    @Override
    public String getDisplayName() {
        return "MongoDB Complex";
    }

    @Override
    public String getDescription() {
        return "MongoDB Complex Database";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[]{
                NAMESPACE,
                DATA_STORE_URI,
                SCHEMA_STORE_URI,
                DATA_STORE_TYPE
        };
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        return params.containsKey(NAMESPACE.getName())
                && params.containsKey(DATA_STORE_URI.getName())
                && params.containsKey(SCHEMA_STORE_URI.getName())
                && params.containsKey(DATA_STORE_TYPE.getName())
                && params.get(DATA_STORE_TYPE.getName()).equals("complex");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    @Override
    public DataAccess<MongoComplexFeatureType, MongoComplexFeature> createDataStore(Map<String, Serializable> parameters) throws IOException {
        return new MongoComplexDataAccess((String)DATA_STORE_URI.lookUp(parameters), (String)SCHEMA_STORE_URI.lookUp(parameters));
    }
}
