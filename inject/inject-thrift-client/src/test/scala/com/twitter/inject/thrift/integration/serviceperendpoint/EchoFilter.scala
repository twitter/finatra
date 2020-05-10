package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.Logging
import com.twitter.test.thriftscala.EchoService.Echo

class EchoFilter
    extends Filter[Echo.Args, Echo.SuccessType, Echo.Args, Echo.SuccessType]
    with Logging {
  override def apply(
    request: Echo.Args,
    service: Service[Echo.Args, String]
  ) = {
    info("Echo called with msg " + request.msg)
    service(request)
  }
}
