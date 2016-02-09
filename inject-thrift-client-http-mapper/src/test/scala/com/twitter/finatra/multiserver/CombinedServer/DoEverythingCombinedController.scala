package com.twitter.finatra.multiserver.CombinedServer

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.{Inject, Singleton}

@Singleton
class DoEverythingCombinedController @Inject()(
  adder: AdderService)
  extends Controller {

  get("/ping") { request: Request =>
    "pong"
  }

  get("/add1") { request: Request =>
    val num = request.getIntParam("num")
    adder.add1(num)
  }

  get("/add1String") { request: Request =>
    val num = request.getParam("num")
    adder.add1String(num)
  }
}
