package com.twitter.inject.thrift.integration.inheritance

import com.google.inject.Module
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.integration.filters.MethodLoggingTypeAgnosticFilter
import com.twitter.inject.thrift.modules.{ServicePerEndpointModule, ThriftClientIdModule}
import com.twitter.serviceA.thriftscala.ServiceA
import com.twitter.serviceB.thriftscala.ServiceB

object ServiceBServicePerEndpointModule
    extends ServicePerEndpointModule[ServiceB.ServicePerEndpoint, ServiceB.MethodPerEndpoint] {

  override val modules: Seq[Module] = Seq(ThriftClientIdModule)

  override val dest = "flag!serviceB-thrift-service"
  override val label = "serviceB-thrift-client"

  override def configureServicePerEndpoint(
    builder: ThriftMethodBuilderFactory[ServiceB.ServicePerEndpoint],
    servicePerEndpoint: ServiceB.ServicePerEndpoint
  ): ServiceB.ServicePerEndpoint = {
    servicePerEndpoint
      .withEcho(
        builder.method[ServiceA.Echo.Args, ServiceA.Echo.SuccessType](ServiceA.Echo)
          .withRetryForClassifier(PossiblyRetryableExceptions)
          .withAgnosticFilter(new MethodLoggingTypeAgnosticFilter())
          .filtered[EchoFilter]
          .service)
      .withPing(
        builder.method(ServiceB.Ping)
          .filtered(new PingFilter)
          .nonIdempotent
          .withRetryDisabled
          .service)
  }
}
