package com.twitter.inject.thrift.integration.serviceperendpoint

import com.google.inject.Module
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.integration.filters.{MethodLoggingTypeAgnosticFilter, SetTimesEchoTypeAgnosticFilter}
import com.twitter.inject.thrift.modules.{PossiblyRetryableExceptions, ThriftClientIdModule, ThriftMethodBuilderClientModule}
import com.twitter.test.thriftscala.EchoService

object EchoThriftMethodBuilderClientModule
  extends ThriftMethodBuilderClientModule[EchoService.ServicePerEndpoint, EchoService.MethodPerEndpoint] {

  override val modules: Seq[Module] = Seq(ThriftClientIdModule)

  override val dest = "flag!echo-thrift-service"
  override val label = "echo-thrift-client"

  override def configureServicePerEndpoint(
    builder: ThriftMethodBuilderFactory[EchoService.ServicePerEndpoint],
    servicePerEndpoint: EchoService.ServicePerEndpoint
  ): EchoService.ServicePerEndpoint = {

    servicePerEndpoint
      .withEcho(
        builder.method[EchoService.Echo.Args, EchoService.Echo.SuccessType](EchoService.Echo)
          // method type-specific filter
          .filtered[EchoFilter]
          .withRetryForClassifier(PossiblyRetryableExceptions)
          .service)
      .withSetTimesToEcho(
        builder.method(EchoService.SetTimesToEcho)
          // method type-agnostic filter
          .withAgnosticFilter(new SetTimesEchoTypeAgnosticFilter())
          .withRetryForClassifier(PossiblyRetryableExceptions)
          .service)
      // global (type-agnostic) filter
      .filtered(new MethodLoggingTypeAgnosticFilter())
  }
}
