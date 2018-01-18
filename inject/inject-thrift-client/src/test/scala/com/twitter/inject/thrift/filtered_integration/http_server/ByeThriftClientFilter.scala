package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.{Filter, Service}
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.inject.Logging
import com.twitter.util.Future

class ByeThriftClientFilter extends Filter[Bye.Args, Bye.SuccessType, Bye.Args, Bye.SuccessType] with Logging {
  def apply(request: Bye.Args, service: Service[Bye.Args, Bye.SuccessType]): Future[Bye.SuccessType] = {
    info("Bye called with name " + request.name)
    service(request)
  }

}
