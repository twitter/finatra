package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.greeter.thriftscala.Greeter.{Bye, Hello, Hi}
import com.twitter.scrooge
import javax.inject.Inject

class ReqRepServicePerEndpointHttpController @Inject() (
  greeter: Greeter.ReqRepServicePerEndpoint)
    extends Controller {

  get("/hi") { request: Request =>
    greeter.hi(scrooge.Request(Hi.Args(name = request.params("name")))).map(_.value)
  }

  get("/hello") { request: Request =>
    greeter.hello(scrooge.Request(Hello.Args(name = request.params("name"))))
  }

  get("/bye") { request: Request =>
    greeter
      .bye(
        scrooge.Request(Bye.Args(name = request.params("name"), age = request.getIntParam("age")))
      )
      .map(_.value.msg)
  }

}
