package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finatra.http.Controller

class NonGuiceController extends Controller {
  get("/NonGuice") { request: FinagleRequest =>
    "pong"
  }
}
