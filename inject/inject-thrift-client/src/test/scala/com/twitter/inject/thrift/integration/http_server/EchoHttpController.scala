package com.twitter.inject.thrift.integration.http_server

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import javax.inject.Inject

class EchoHttpController @Inject()(
  echoThriftService: EchoService[Future])
  extends Controller {

  get("/echo") { request: Request =>
    val msg = request.params("msg")
    echoThriftService.echo(msg)
  }

  post("/config") { request: Request =>
    val timesToEcho = request.params("timesToEcho").toInt
    echoThriftService.setTimesToEcho(timesToEcho)
  }
}
