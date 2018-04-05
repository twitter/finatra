package com.twitter.inject.thrift.integration.inheritance

import com.google.inject.Module
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.integration.filters.MethodLoggingTypeAgnosticFilter
import com.twitter.inject.thrift.modules.{ThriftClientIdModule, ThriftMethodBuilderClientModule}
import com.twitter.inject.Injector
import com.twitter.serviceA.thriftscala.ServiceA
import com.twitter.serviceB.thriftscala.ServiceB

object ServiceBThriftMethodBuilderClientModule
  extends ThriftMethodBuilderClientModule[ServiceB.ServicePerEndpoint, ServiceB.MethodPerEndpoint] {

  override val modules: Seq[Module] = Seq(ThriftClientIdModule)

  override val dest = "flag!serviceB-thrift-service"
  override val label = "serviceB-thrift-client"

  override protected def configureServicePerEndpoint(
    injector: Injector,
    builder: ThriftMethodBuilderFactory[ServiceB.ServicePerEndpoint],
    servicePerEndpoint: ServiceB.ServicePerEndpoint
  ): ServiceB.ServicePerEndpoint = {
    servicePerEndpoint
      .withEcho(
        builder.method[ServiceA.Echo.Args, ServiceA.Echo.SuccessType](ServiceA.Echo)
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
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
