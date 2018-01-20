package com.twitter.inject.thrift.integration.inheritance

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.serviceB.thriftscala.ServiceB
import javax.inject.Inject

class ServiceBHttpController @Inject()(serviceB: ServiceB.MethodPerEndpoint) extends Controller {

  get("/echo") { request: Request =>
    serviceB.echo(request.params("msg"))
  }

  get("/ping") { _: Request =>
    serviceB.ping()
  }
}
