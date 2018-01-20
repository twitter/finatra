package com.twitter.inject.thrift.integration.inheritance

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.Logging
import com.twitter.serviceA.thriftscala.ServiceA
import com.twitter.util.Future

class EchoFilter
  extends Filter[
    ServiceA.Echo.Args,
    ServiceA.Echo.SuccessType,
    ServiceA.Echo.Args,
    ServiceA.Echo.SuccessType]
  with Logging {
  def apply(
    request: ServiceA.Echo.Args,
    service: Service[ServiceA.Echo.Args, String]
  ): Future[ServiceA.Echo.SuccessType] = {
    info("Echo called with name " + request.msg)
    service(request)
  }
}
