package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Hi
import com.twitter.greeter.thriftscala.Greeter.Hi.{Args, Result}
import com.twitter.inject.Logging
import com.twitter.util.Future

class HiLoggingThriftClientFilter extends ThriftClientFilter[Hi.Args, Hi.Result] with Logging {
  
  override def apply(request: Args, service: Service[Args, Result]): Future[Result] = {
    info("Hi called with name " + request.name)
    service(request)
  }
}
