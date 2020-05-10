package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.greeter.thriftscala.Greeter.{Bye, Hello, Hi}
import com.twitter.test.thriftscala.EchoService
import com.twitter.test.thriftscala.EchoService.{Echo, SetTimesToEcho}
import javax.inject.Inject

class ServicePerEndpointHttpController @Inject() (
  greeter: Greeter.ServicePerEndpoint,
  echo: EchoService.ServicePerEndpoint)
    extends Controller {

  get("/hi") { request: Request =>
    greeter.hi(Hi.Args(name = request.params("name")))
  }

  get("/hello") { request: Request =>
    greeter.hello(Hello.Args(name = request.params("name")))
  }

  get("/bye") { request: Request =>
    greeter
      .bye(Bye.Args(name = request.params("name"), age = request.getIntParam("age"))).map(_.msg)
  }

  get("/echo") { request: Request =>
    val msg = request.params("msg")
    echo.echo(Echo.Args(msg = msg))
  }

  post("/config") { request: Request =>
    val timesToEcho = request.params("timesToEcho").toInt
    echo.setTimesToEcho(SetTimesToEcho.Args(times = timesToEcho))
  }
}
