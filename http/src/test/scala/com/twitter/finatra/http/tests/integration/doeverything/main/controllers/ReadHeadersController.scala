package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.AppendToHeaderFilter

class ReadHeadersController extends Controller {

  filter(new AppendToHeaderFilter("test", "3")).
    filter(new AppendToHeaderFilter("test", "4")).
    filter(new AppendToHeaderFilter("test", "5")).
    get("/multipleRouteFilters") { r: Request =>
      r.headerMap("test")
  }

}
