package com.twitter.finatra.http.tests.integration.main;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.AbstractController;

/**
 * Test all HTTP methods
 */
public class DoEverythingJavaController extends AbstractController {
    private final HelloService helloService;

    @Inject
    public DoEverythingJavaController(HelloService helloService) {
        this.helloService = helloService;
    }

    /** Define routes */
    public void configureRoutes() {
        get("/hello", (Request request) -> helloService.hi(request.getParam("name")));

        get("/goodbye", (Request request) -> new GoodbyeResponse("guest", "cya", 123));

        get("/query", request ->
            helloService.computeQueryResult(request.getParam("q")));

        post("/post",  (Request request) -> "post");

        put("/put",  (Request request) -> "put");

        delete("/delete",  (Request request) -> "delete");

        options("/options",  (Request request) -> "options");

        patch("/patch",  (Request request) -> "patch");

        head("/head",  (Request request) -> "head");

        any("/any",  (Request request) -> "any");
    }
}
