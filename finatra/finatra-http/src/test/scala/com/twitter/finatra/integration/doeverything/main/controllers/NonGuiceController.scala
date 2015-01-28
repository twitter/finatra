package com.twitter.finatra.integration.doeverything.main.controllers

import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finatra.Controller


class NonGuiceController extends Controller {
  get("/NonGuice") { request: FinagleRequest =>
    "pong"
  }
}