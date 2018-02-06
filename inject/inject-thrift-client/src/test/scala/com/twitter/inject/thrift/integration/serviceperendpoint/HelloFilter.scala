package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.{Filter, Service}
import com.twitter.greeter.thriftscala.Greeter.Hello
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class HelloFilter
  extends Filter[
    Hello.Args,
    Hello.SuccessType,
    Hello.Args,
    Hello.SuccessType]
  with Logging {
  def apply(
    request: Hello.Args,
    service: Service[Hello.Args, Hello.SuccessType]
  ): Future[Hello.SuccessType] = {
    info("Hello called with name " + request.name)
    service(request)
  }
}
