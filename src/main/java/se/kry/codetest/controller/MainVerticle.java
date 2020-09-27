package se.kry.codetest.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.persistence.DBConnector;
import se.kry.codetest.service.BackgroundPoller;
import se.kry.codetest.service.CRUDService;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainVerticle extends AbstractVerticle {

    //TODO use this
    private DBConnector dbConnector;
    private BackgroundPoller poller;
    private Pattern pattern;
    private CRUDService crudService;

    @Override
    public void start(Future<Void> startFuture) {

        dbConnector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        poller = new BackgroundPoller(vertx, dbConnector);
        crudService = new CRUDService(dbConnector);

        String pattern = "[a-zA-Z_0-9]+\\.[a-zA-Z0-9]{2,3}(/|[-a-zA-Z0-9@:%._\\\\+~#?&//=]*)";
        this.pattern = Pattern.compile(pattern);
        settingUpCookie(router);
        vertx.setPeriodic(100 * 600, timerId -> poller.pollServices());
        setRoutes(router);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started...");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void settingUpCookie(Router router) {

        // This cookie handler will be called for all routes
        router.route().handler(CookieHandler.create());

        // on every path call, the cookie is evaluated
        router.route().handler(ctx -> {

            Cookie clientCookieId = ctx.getCookie("remoteLiviClient");

            String cookieValue;
            if (clientCookieId == null) {
                cookieValue = UUID.randomUUID().toString();
            }else{
                cookieValue = clientCookieId.getValue();
            }

            // Add a cookie - this will get written back in the response automatically
            ctx.addCookie(Cookie.cookie("remoteLiviClient", cookieValue));
            ctx.next();
        });
    }

    private void setRoutes(Router router) {

        router.route("/*").handler(StaticHandler.create());

        router.get("/service").handler(req -> {

            Cookie remoteLiviClientCookie = req.getCookie("remoteLiviClient");
            JsonArray jsonArray = new JsonArray()
                    .add(remoteLiviClientCookie.getValue());
            Future<ResultSet> allServices = crudService.getAllServicesByUser(jsonArray);

            allServices.setHandler(resultDb -> {

                if (resultDb.succeeded()) {
                    String msg;
                    if (resultDb.result().getResults().size() > 0) {
                        msg = Json.encodePrettily(allServices.result().getRows());
                    } else {
                        msg = "Database has no data! Insert something first!!!";
                    }
                    req.response()
                            .putHeader("content-type", "application/json")
                            .end(msg);
                } else {
                    setupResponse(500, req, "Failure fetching service!!");
                }
            });
        });

//        router.post("/service").handler(req -> {
        router.post("/service").blockingHandler(req-> {

            JsonObject jsonBody = req.getBodyAsJson();
            Matcher matcher = pattern.matcher(jsonBody.getString("url"));

            if (matcher.matches()) {
                System.out.println("URL is valid!! cheers!!");
                Cookie remoteLiviClientCookie = req.getCookie("remoteLiviClient");
                JsonArray jsonArray = new JsonArray()
                        .add(jsonBody.getString("name"))
                        .add(jsonBody.getString("url"))
                        .add(remoteLiviClientCookie.getValue());

                Future<ResultSet> futureInsert = crudService.insertService(jsonArray);

                futureInsert.setHandler(dbReq -> {

                    if (dbReq.succeeded()) {
                        setupResponse(201, req, "Successful Insertion of service!\n" + jsonArray.toString());
                    } else {
                        setupResponse(400, req, "Failure saving service in DB!\n" + dbReq.cause() + "\n" + jsonArray.toString());
                    }
                });
            } else {
                System.out.println("The URL does not follow any common pattern, rewrite it please!");
                req.response().
                        setStatusCode(400)
                        .setStatusMessage("Bad URL formatting")
                        .end("The URL does not follow any common pattern, rewrite it removing www. http or https! e.g. host/uri");
            }
        }, true);


        router.delete("/service/:serviceName").produces("*/json").handler(req -> {

            String serviceName = req.request().getParam("serviceName");
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(serviceName);

            Future<UpdateResult> futureDeletion = crudService.deleteService(jsonArray);

            futureDeletion.setHandler(dbReq -> {
                if (dbReq.succeeded()) {
                    if (dbReq.result().getUpdated() > 0) {
                        setupResponse(200, req, "Successful DELETION of service with name: " + serviceName);
                    } else {
                        setupResponse(200, req, "It was not deleted any service with name: "+serviceName+". First make a get() to see some names!!!");
                    }
                } else {
                    setupResponse(500, req, "Failure DELETING service with name: " + serviceName + " .Cause:\n" + dbReq.cause());
                }
            });
        });


        router.patch("/service/:currentServiceName/:newServiceName").handler(req -> {

            String currentServiceName = req.request().getParam("currentServiceName");
            String newServiceName = req.request().getParam("newServiceName");

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(newServiceName);
            jsonArray.add(currentServiceName);

            Future<UpdateResult> updateResultFuture = crudService.updateServiceName(jsonArray);

            updateResultFuture.setHandler(dbReq -> {

                if (dbReq.succeeded()) {
                    if (dbReq.result().getUpdated() > 0) {
                        setupResponse(200, req, "Successful UPDATED of service with name: " + currentServiceName + ". New name:" + newServiceName);
                    } else {
                        setupResponse(200, req, "None service UPDATED with current name: " + currentServiceName+". First make a get() to see some names!!!");
                    }
                } else {
                    setupResponse(501, req, "Failure or problem UPDATING service with name: " + currentServiceName);
                }
            });
        });
    }

    private void setupResponse(int statusCode, RoutingContext req, String chunk) {
        System.out.println("Setting response and headers...");
        req.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "text/plain")
                .end(chunk);
    }
}
