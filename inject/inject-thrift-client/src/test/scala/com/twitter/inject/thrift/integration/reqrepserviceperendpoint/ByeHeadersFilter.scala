package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.scrooge
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Singleton

@Singleton
class ByeHeadersFilter(
  requestHeaderKey: String)
    extends Filter[scrooge.Request[Bye.Args], scrooge.Response[Bye.SuccessType], scrooge.Request[
      Bye.Args
    ], scrooge.Response[Bye.SuccessType]]
    with Logging {
  def apply(
    request: scrooge.Request[Bye.Args],
    service: Service[scrooge.Request[Bye.Args], scrooge.Response[Bye.SuccessType]]
  ): Future[scrooge.Response[Bye.SuccessType]] = {
    service(request.setHeader(requestHeaderKey, "bye"))
  }
}
