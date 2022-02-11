package com.twitter.inject.thrift.integration.inheritance

import com.twitter.finagle.Service
import com.twitter.inject.thrift.integration.AbstractThriftService
import com.twitter.serviceA.thriftscala.ServiceA
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.util.Future
import com.twitter.util.logging.Logging

class ServiceBThriftService(
  clientId: String)
    extends AbstractThriftService
    with ServiceB.ServicePerEndpoint
    with Logging {

  val ping: Service[ServiceB.Ping.Args, ServiceB.Ping.SuccessType] =
    Service.mk { args: ServiceB.Ping.Args =>
      assertClientId(clientId)
      Future.value("pong")
    }

  val echo: Service[ServiceA.Echo.Args, ServiceA.Echo.SuccessType] =
    Service.mk { args: ServiceA.Echo.Args =>
      assertClientId(clientId)
      Future.value(args.msg)
    }
}
