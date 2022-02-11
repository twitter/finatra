package com.twitter.inject.thrift.integration.inheritance

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.serviceB.thriftscala.ServiceB.Ping
import com.twitter.util.Future
import com.twitter.util.logging.Logging

class PingFilter
    extends Filter[Ping.Args, Ping.SuccessType, Ping.Args, Ping.SuccessType]
    with Logging {
  def apply(
    request: Ping.Args,
    service: Service[Ping.Args, String]
  ): Future[Ping.SuccessType] = {
    info("Ping called")
    service(request)
  }
}
