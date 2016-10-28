package com.twitter.finatra.http.tests.integration.darktraffic.main

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class DarkTrafficTestController extends Controller {
  get("/plaintext") { request: Request =>
    "Hello, World!"
  }

  put("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  post("/foo") { request: Request =>
    "bar"
  }

  delete("/delete") { request: Request =>
    "delete"
  }
}
