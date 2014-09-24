package com.twitter.hello

import com.twitter.finatra.{Controller, Request}

class HelloWorldController extends Controller {

  get("/ping") { request: Request =>
    "Hello World!"
  }
}
