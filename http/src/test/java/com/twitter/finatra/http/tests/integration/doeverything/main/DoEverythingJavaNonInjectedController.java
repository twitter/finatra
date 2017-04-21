package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.AbstractController;

public class DoEverythingJavaNonInjectedController extends AbstractController {
    private final HelloService helloService;

    public DoEverythingJavaNonInjectedController(HelloService helloService) {
        this.helloService = helloService;
    }

    /** Define routes */
    public void configureRoutes() {
        get("/nonInjected/hello", (Request request) ->
            helloService.hi(request.getParam("name")));

        get("/nonInjected/response", (Request request) ->
            response().ok(helloService.hi(request.getParam("name"))));
    }
}
