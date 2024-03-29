package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.Greeter.Hi
import com.twitter.scrooge
import com.twitter.util.Future
import com.twitter.util.logging.Logging

class HiHeadersFilter(
  requestHeaderKey: String)
    extends Filter[scrooge.Request[Hi.Args], scrooge.Response[Hi.SuccessType], scrooge.Request[
      Hi.Args
    ], scrooge.Response[Hi.SuccessType]]
    with Logging {
  def apply(
    request: scrooge.Request[Hi.Args],
    service: Service[scrooge.Request[Hi.Args], scrooge.Response[Hi.SuccessType]]
  ): Future[scrooge.Response[Hi.SuccessType]] = {
    service(request.setHeader(requestHeaderKey, "hi"))
  }
}
