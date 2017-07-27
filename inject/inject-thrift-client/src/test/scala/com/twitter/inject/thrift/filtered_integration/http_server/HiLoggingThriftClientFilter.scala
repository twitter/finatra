package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Hi
import com.twitter.inject.Logging
import com.twitter.util.Future

class HiLoggingThriftClientFilter extends ThriftClientFilter[Hi.Args, Hi.SuccessType] with Logging {

  def apply(request: Hi.Args, service: Service[Hi.Args, Hi.SuccessType]): Future[Hi.SuccessType] = {
    info("Hi called with name " + request.name)
    service(request)
  }
}
