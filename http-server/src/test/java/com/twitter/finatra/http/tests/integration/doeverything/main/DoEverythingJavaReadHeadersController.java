package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.AbstractController;

public class DoEverythingJavaReadHeadersController extends AbstractController {

    /** Define routes */
    public void configureRoutes() {

        get("/headers", (Request request) ->
                request.headerMap().apply("test"));
    }

}
