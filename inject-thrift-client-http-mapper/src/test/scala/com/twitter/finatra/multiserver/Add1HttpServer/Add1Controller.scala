package com.twitter.finatra.multiserver.Add1HttpServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import javax.inject.Inject

class Add1Controller @Inject()(
  adder: Adder[Future])
  extends Controller {

  get("/add1") { request: Request =>
    val num = request.getIntParam("num")
    adder.add1(num)
  }

  get("/add1String") { request: Request =>
    val num = request.getParam("num")
    adder.add1String(num)
  }

  get("/slowAdd1") { request: Request =>
    val num = request.getParam("num")
    adder.add1Slowly(num)
  }

  get("/errorAdd1") { request: Request =>
    val num = request.getParam("num")
    adder.add1AlwaysError(num)
  }
}