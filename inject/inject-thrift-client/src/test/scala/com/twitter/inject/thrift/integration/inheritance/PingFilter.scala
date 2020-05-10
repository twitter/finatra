package com.twitter.inject.thrift.integration.inheritance

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.Logging
import com.twitter.serviceB.thriftscala.ServiceB.Ping
import com.twitter.util.Future

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
