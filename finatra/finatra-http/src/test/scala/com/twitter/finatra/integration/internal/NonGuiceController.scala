package com.twitter.finatra.integration.internal

import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finatra.Controller


class NonGuiceController extends Controller {
  get("/legacy") { request: FinagleRequest =>
    "pong"
  }
}