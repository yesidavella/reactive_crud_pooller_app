package se.kry.codetest.service;

import io.netty.handler.codec.http.HttpStatusClass;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.client.WebClient;
import se.kry.codetest.persistence.DBConnector;

public class BackgroundPoller {

    private WebClient webClient;
    private DBConnector dbConnector;

    private enum HttpResponseStatus {OK, FAIL}

    public BackgroundPoller(Vertx vertx, DBConnector dbConnector) {
        webClient = WebClient.create(vertx);
        this.dbConnector = dbConnector;
    }

    public void pollServices() {

        Future<ResultSet> allServices = dbConnector.getAllServices();

        allServices.setHandler(dbReq -> {

            if (dbReq.succeeded()) {

                dbReq.result().getResults().stream().forEach(

                        service -> {

                            System.out.println(String.format("Beginning URL:%s EVALUATION with currentStatus: %s", service.getString(1), service.getString(2)));

                            webClient.get(getHost(service.getString(1)), getUri(service.getString(1)))
//                            webClient.get(service.getString(1)).port(80)
                                    .timeout(2300)
                                    .send(response -> {

                                        final String statusBeforeCall = service.getString(2);
                                        final String statusAfterCall;

                                        if (response.succeeded()) {
                                            System.out.println("Succeeded calling URL:" + service.getString(1) + " Code:" + response.result().statusCode());
                                            if (HttpStatusClass.SUCCESS.contains(response.result().statusCode())) {
                                                System.out.println("The URL answered with a SUCCESS code");
                                                statusAfterCall = HttpResponseStatus.OK.toString();
                                            } else {
                                                System.out.println("The URL answered a FAILURE code");
                                                statusAfterCall = HttpResponseStatus.FAIL.toString();
                                            }
                                        } else {
                                            statusAfterCall = HttpResponseStatus.FAIL.toString();
                                            System.out.println("Failure REASON: " + response.cause().getMessage());
                                        }

                                        if (!statusBeforeCall.equals(statusAfterCall)) {
                                            System.out.println("Changing STATUS...!!");
                                            JsonArray jsonArray = new JsonArray();
                                            jsonArray.add(statusAfterCall);
                                            jsonArray.add(service.getString(0));

                                            Future<UpdateResult> updateResultFuture = dbConnector.updateServiceStatus(jsonArray);
                                            updateResultFuture.setHandler(req -> {
                                                if (req.succeeded()) {
                                                    if (req.result().getUpdated() > 0) {
                                                        System.out.println(String.format("UPDATED URL:%s was:%s and now:%s", service.getString(1), statusBeforeCall, statusAfterCall));
                                                    } else {
                                                        System.out.println(String.format("NO UPDATED URL:%s was:%s and now:%s", service.getString(1), statusBeforeCall, statusAfterCall));
                                                    }
                                                } else {
                                                    System.out.println(String.format("FAILURE UPDATING URL:%s was:%s and now:%s because %s", service.getString(1), statusBeforeCall, statusAfterCall, updateResultFuture.cause().toString()));
                                                }
                                            });
                                        }
                                    });
                        });
            } else {
                System.out.println("Poller FAILED QUERYING DATABASE!! Wake up an run away!!");
            }
        });
    }

    private String getUri(String string) {
        int i = (string.indexOf("/") <= 0) ? string.length() : string.indexOf("/");
        return string.substring(i);
    }

    private String getHost(String string) {
        int i = (string.indexOf("/") <= 0) ? string.length() : string.indexOf("/");
        return string.substring(0, i);
    }
}