package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.AbstractController;

public class DoEverythingJavaReadHeadersInjectedController extends AbstractController {
  /** Define routes */
  public void configureRoutes() {

    get("/injected/headers", (Request request) ->
        request.headerMap().apply("test"));
  }

}
