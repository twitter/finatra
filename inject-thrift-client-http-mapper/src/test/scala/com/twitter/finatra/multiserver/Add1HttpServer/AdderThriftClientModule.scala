package com.twitter.finatra.multiserver.Add1HttpServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Filter
import com.twitter.inject.Injector
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule

object AdderThriftClientModule
    extends ThriftMethodBuilderClientModule[Adder.ServicePerEndpoint, Adder.MethodPerEndpoint] {

  override val label = "adder-thrift"
  override val dest = "flag!adder-thrift-server"

  override def configureServicePerEndpoint(
    injector: Injector,
    builder: ThriftMethodBuilderFactory[Adder.ServicePerEndpoint],
    servicePerEndpoint: Adder.ServicePerEndpoint
  ): Adder.ServicePerEndpoint = {

    servicePerEndpoint
      .withAdd1(
        builder
          .method(Add1)
          .withExceptionFilter(Filter.identity[Add1.Args, Add1.SuccessType])
          .withTimeoutTotal(3.minutes)
          .withTimeoutPerRequest(1.minute)
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
          .withMaxRetries(3)
          .service)
      .withAdd1String(
        builder
          .method(Add1String)
          .withTimeoutTotal(3.minutes)
          .withTimeoutPerRequest(1.minute)
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
          .withMaxRetries(3)
          .service
      ).withAdd1Slowly(
        builder
          .method(Add1Slowly)
          .withTimeoutTotal(3.minutes)
          // We purposely set a very small timeout so that we can test handling IndividualRequestTimeoutException
          .withTimeoutPerRequest(1.millis)
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
          .withMaxRetries(3)
          .service
      ).withAdd1AlwaysError(
        builder
          .method(Add1AlwaysError)
          .withTimeoutTotal(3.minutes)
          .withTimeoutPerRequest(1.minute)
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
          .withMaxRetries(3)
          .service
      )
  }
}
