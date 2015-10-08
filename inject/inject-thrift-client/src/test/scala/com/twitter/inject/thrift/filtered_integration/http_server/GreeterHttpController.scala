package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.util.Future
import javax.inject.Inject

class GreeterHttpController @Inject()(
  greeter: Greeter[Future])
  extends Controller {

  get("/hi") { request: Request =>
    greeter.hi(
      name = request.params("name"))
  }

  get("/bye") { request: Request =>
    greeter.bye(
      name = request.params("name"),
      age = request.getIntParam("age")) map {_.msg}
  }
}
