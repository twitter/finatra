package com.twitter.inject.thrift.integration.inheritance

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.serviceA.thriftscala.ServiceA
import com.twitter.util.Future
import com.twitter.util.logging.Logging

class EchoFilter
    extends Filter[
      ServiceA.Echo.Args,
      ServiceA.Echo.SuccessType,
      ServiceA.Echo.Args,
      ServiceA.Echo.SuccessType
    ]
    with Logging {
  def apply(
    request: ServiceA.Echo.Args,
    service: Service[ServiceA.Echo.Args, String]
  ): Future[ServiceA.Echo.SuccessType] = {
    info("Echo called with name " + request.msg)
    service(request)
  }
}
