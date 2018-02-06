package com.twitter.inject.thrift.integration.filtered

import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.inject.Logging
import com.twitter.util.Future

class ByeThriftClientFilter extends ThriftClientFilter[Bye.Args, Bye.SuccessType] with Logging {
  def apply(request: Bye.Args, service: Service[Bye.Args, Bye.SuccessType]): Future[Bye.SuccessType] = {
    info("Bye called with name " + request.name)
    service(request)
  }

}
