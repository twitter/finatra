package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.test.thriftscala.EchoService.Echo
import com.twitter.util.logging.Logging

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
