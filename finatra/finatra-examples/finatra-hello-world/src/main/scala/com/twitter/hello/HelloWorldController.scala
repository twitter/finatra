package com.twitter.hello

import com.twitter.finatra.{Controller, Request}

class HelloWorldController extends Controller {

  get("/hi") { request: Request =>
    "yo"
  }
}
