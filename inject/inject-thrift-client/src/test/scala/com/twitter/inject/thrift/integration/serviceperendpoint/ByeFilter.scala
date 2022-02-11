package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.util.Future
import com.twitter.util.logging.Logging
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
