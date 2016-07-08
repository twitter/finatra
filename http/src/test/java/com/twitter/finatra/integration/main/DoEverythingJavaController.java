package com.twitter.finatra.integration.main;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.JavaController;

/**
 * Test all HTTP methods
 */
public class DoEverythingJavaController extends JavaController {

    @Inject
    private HelloService helloService;

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
