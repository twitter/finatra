package com.twitter.finatra.http.integration.doeverything.main.controllers

import com.twitter.finagle.httpx.{Request => FinagleRequest}
import com.twitter.finatra.http.Controller

class NonGuiceController extends Controller {
  get("/NonGuice") { request: FinagleRequest =>
    "pong"
  }
}
