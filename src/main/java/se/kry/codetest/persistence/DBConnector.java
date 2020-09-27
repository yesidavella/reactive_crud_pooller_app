package se.kry.codetest.persistence;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;

public class DBConnector {

    private final String DB_PATH = "poller.db";
    private final SQLClient dbClient;

    public DBConnector(Vertx vertx) {

        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + DB_PATH)
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30);

        dbClient = JDBCClient.createShared(vertx, config);
        createTableIfNotExist();
    }

    private void createTableIfNotExist() {

        String createServiceTableStm = "CREATE TABLE IF NOT EXISTS service (" +
                "  name VARCHAR(64) NOT NULL UNIQUE," +
                "  url VARCHAR(128) NOT NULL," +
                "  status VARCHAR(16) DEFAULT 'UNDEFINED'," +
                "  creation_date DATETIME NOT NULL," +
                "  user_cookie VARCHAR(64));";

        query(createServiceTableStm).setHandler(done -> {
            if (done.succeeded()) {
                System.out.println("Cheers!! Service table READY TO BE USED!!");
            } else {
                done.cause().printStackTrace();
            }
//      vertx.close(shutdown -> {
//        System.out.println("Shutting down database!!");
//        System.exit(0);
//      });
        });
    }

    public Future<ResultSet> query(String query) {
        return query(query, new JsonArray());
    }

    public Future<ResultSet> query(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        Future<ResultSet> queryResultFuture = Future.future();

        SQLClient s = dbClient.queryWithParams(query, params, result -> {

            if (result.failed()) {
                System.out.println(result.toString());
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });

        return queryResultFuture;
    }

    public Future<UpdateResult> update(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        Future<UpdateResult> updateResultFuture = Future.future();

        dbClient.updateWithParams(query, params, result -> {

            if (result.failed()) {
                updateResultFuture.fail(result.cause());
            } else {
                updateResultFuture.complete(result.result());
            }
        });

        return updateResultFuture;
    }


    public Future<ResultSet> getAllServicesByUser(JsonArray serviceInfo) {
        String selectStmt = "SELECT * FROM service WHERE user_cookie=?;";
        Future<ResultSet> future = query(selectStmt, serviceInfo);
        return future;
    }

    public Future<ResultSet> getAllServices() {
        String selectStmt = "SELECT * FROM service;";
        Future<ResultSet> future = query(selectStmt);
        return future;
    }

    public Future<ResultSet> insertService(JsonArray serviceInfo) {
        String insertStm = "INSERT INTO service (name,url,status,creation_date,user_cookie) values (?,?,'UNDEFINED',datetime('now'),?);";
        Future<ResultSet> future = query(insertStm, serviceInfo);
        return future;
    }

    public Future<UpdateResult> deleteService(JsonArray serviceInfo) {
        String deleteStm = "DELETE FROM service WHERE name=?;";
        Future<UpdateResult> future = update(deleteStm, serviceInfo);
        return future;
    }

    public Future<UpdateResult> updateServiceName(JsonArray serviceInfo) {
        String updateStm = "UPDATE service SET name=? WHERE name=?;";
        Future<UpdateResult> future = update(updateStm, serviceInfo);
        return future;
    }

    public Future<UpdateResult> updateServiceStatus(JsonArray serviceInfo) {
        String updateStm = "UPDATE service SET status=? WHERE name=?;";
        Future<UpdateResult> future = update(updateStm, serviceInfo);
        return future;
    }
}
