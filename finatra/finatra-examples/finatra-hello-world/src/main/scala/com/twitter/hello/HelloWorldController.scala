package com.twitter.hello

import com.twitter.finagle.http.Request
import com.twitter.finatra.Controller

class HelloWorldController extends Controller {

  get("/hi") { request: Request =>
    "Hello " + request.params("name")
  }
}
