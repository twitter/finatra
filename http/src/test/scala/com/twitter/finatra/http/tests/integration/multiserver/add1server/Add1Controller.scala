package com.twitter.finatra.http.tests.integration.multiserver.add1server

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class Add1Controller extends Controller {
  get("/add1") { request: Request =>
    request.getIntParam("num") + 1
  }
}