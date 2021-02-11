package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.http.AbstractController;

public class DoEverythingJavaNonInjectedController extends AbstractController {

  /**
   * Define routes
   */
  public void configureRoutes() {
    get("/nonInjected/hello", (Request request) -> "Hello " + request.getParam("name"));
    get("/nonInjected/response", (Request request) -> "Hello " + request.getParam("name"));
  }
}
