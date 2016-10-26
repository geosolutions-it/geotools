package org.geotools.data.mongodb.complex;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.ServiceInfo;
import org.opengis.feature.type.Name;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MongoComplexDataAccess implements DataAccess<MongoComplexFeatureType, MongoComplexFeature> {

    final MongoClient dataStoreClient;
    final DB dataStoreDB;

    public MongoComplexDataAccess(String dataStoreURI, String schemaStoreURI) {

        MongoClientURI dataStoreClientURI = createMongoClientURI(dataStoreURI);
        dataStoreClient = createMongoClient(dataStoreClientURI);
        dataStoreDB = createDB(dataStoreClient, dataStoreClientURI.getDatabase(), true);
    }

    final MongoClientURI createMongoClientURI(String dataStoreURI) {
        if (dataStoreURI == null) {
            throw new IllegalArgumentException("dataStoreURI may not be null");
        }
        if (!dataStoreURI.startsWith("mongodb://")) {
            throw new IllegalArgumentException("incorrect scheme for URI, expected to begin with \"mongodb://\", found URI of \"" + dataStoreURI + "\"");
        }
        return new MongoClientURI(dataStoreURI.toString());
    }

    final MongoClient createMongoClient(MongoClientURI mongoClientURI) {
        try {
            return new MongoClient(mongoClientURI);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown mongodb host(s)", e);
        }
    }

    final DB createDB(MongoClient mongoClient, String databaseName, boolean databaseMustExist) {
        if (databaseMustExist && !mongoClient.getDatabaseNames().contains(databaseName)) {
            return null;
        }
        return mongoClient.getDB(databaseName);
    }

    @Override
    public ServiceInfo getInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createSchema(MongoComplexFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSchema(Name typeName, MongoComplexFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Name> getNames() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public MongoComplexFeatureType getSchema(Name name) throws IOException {
        return new MongoComplexFeatureType();
    }

    @Override
    public FeatureSource<MongoComplexFeatureType, MongoComplexFeature> getFeatureSource(Name typeName) throws IOException {
        return new MongoComplexFeatureSource(dataStoreDB.getCollection("datex"));
    }

    @Override
    public void dispose() {
    }
}
