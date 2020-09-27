package se.kry.codetest.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import se.kry.codetest.persistence.DBConnector;

public class CRUDService {

    private DBConnector dbConn;

    public CRUDService(DBConnector connector) {
        dbConn = connector;
    }

    public Future<ResultSet> getAllServicesByUser(JsonArray serviceInfo) {
        return dbConn.getAllServicesByUser(serviceInfo);
    }

    public Future<ResultSet> insertService(JsonArray serviceInfo) {
        return dbConn.insertService(serviceInfo);
    }

    public Future<UpdateResult> deleteService(JsonArray serviceInfo) {
        return dbConn.deleteService(serviceInfo);
    }

    public Future<UpdateResult> updateServiceName(JsonArray serviceInfo) {
        return dbConn.updateServiceName(serviceInfo);
    }
}
