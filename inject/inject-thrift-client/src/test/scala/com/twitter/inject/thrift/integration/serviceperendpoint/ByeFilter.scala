package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.{Filter, Service}
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class ByeFilter extends Filter[Bye.Args, Bye.SuccessType, Bye.Args, Bye.SuccessType] with Logging {
  def apply(
    request: Bye.Args,
    service: Service[Bye.Args, Bye.SuccessType]
  ): Future[Bye.SuccessType] = {
    info("Bye called with name " + request.name)
    service(request)
  }
}
