package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Hello
import com.twitter.inject.Logging
import com.twitter.util.Future

class HelloThriftClientFilter extends ThriftClientFilter[Hello.Args, Hello.SuccessType] with Logging {

  def apply(request: Hello.Args, service: Service[Hello.Args, Hello.SuccessType]): Future[Hello.SuccessType] = {
    info("Hello called with name " + request.name)
    service(request)
  }
}
