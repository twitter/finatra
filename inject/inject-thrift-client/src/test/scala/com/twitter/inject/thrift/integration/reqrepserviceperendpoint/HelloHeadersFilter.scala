package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.twitter.finagle.{Filter, Service}
import com.twitter.greeter.thriftscala.Greeter.Hello
import com.twitter.inject.Logging
import com.twitter.scrooge
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class HelloHeadersFilter(
  requestHeaderKey: String
) extends Filter[
  scrooge.Request[Hello.Args],
  scrooge.Response[Hello.SuccessType],
  scrooge.Request[Hello.Args],
  scrooge.Response[Hello.SuccessType]]
  with Logging {
  def apply(
    request: scrooge.Request[Hello.Args],
    service: Service[scrooge.Request[Hello.Args], scrooge.Response[Hello.SuccessType]]
  ): Future[scrooge.Response[Hello.SuccessType]] = {
    service(request.setHeader(requestHeaderKey, "hello"))
  }
}
