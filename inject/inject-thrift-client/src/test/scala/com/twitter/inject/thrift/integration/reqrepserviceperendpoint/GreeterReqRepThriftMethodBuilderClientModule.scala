package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.google.inject.Module
import com.twitter.conversions.percent._
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.greeter.thriftscala.{Greeter, InvalidOperation}
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.integration.filters.{HiLoggingTypeAgnosticFilter, MethodLoggingTypeAgnosticFilter}
import com.twitter.inject.thrift.modules.{ThriftClientIdModule, ThriftMethodBuilderClientModule}
import com.twitter.inject.Injector
import com.twitter.util.tunable.Tunable
import com.twitter.util.{Duration, Return, Throw}
import scala.util.control.NonFatal

class GreeterReqRepThriftMethodBuilderClientModule(
  requestHeaderKey: String,
  timeoutPerRequestTunable: Tunable[Duration]
) extends ThriftMethodBuilderClientModule[Greeter.ReqRepServicePerEndpoint, Greeter.MethodPerEndpoint] {

  override val modules: Seq[Module] = Seq(ThriftClientIdModule)

  override val dest = "flag!greeter-thrift-service"
  override val label = "greeter-thrift-client"

  override protected def configureServicePerEndpoint(
    injector: Injector,
    builder: ThriftMethodBuilderFactory[Greeter.ReqRepServicePerEndpoint],
    servicePerEndpoint: Greeter.ReqRepServicePerEndpoint
  ): Greeter.ReqRepServicePerEndpoint = {
    servicePerEndpoint
      .withHi(
        builder
          .method(Greeter.Hi)
          .withTimeoutPerRequest(timeoutPerRequestTunable)
          // method type-agnostic filter
          .withAgnosticFilter(new HiLoggingTypeAgnosticFilter)
          // method type-specific filter
          .filtered(new HiHeadersFilter(requestHeaderKey))
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
          .idempotent(1.percent)
          .service
      )
      .withHello(
        builder
          .method(Greeter.Hello)
          // method type-specific filter
          .filtered(new HelloHeadersFilter(requestHeaderKey))
          // method type-specific filter
          .filtered[HelloFilter]
          .withRetryForClassifier(PossiblyRetryable.ResponseClassifier)
          .service
      )
      .withBye(
        builder
          .method(Greeter.Bye)
          // method type-specific filter
          .filtered(new ByeHeadersFilter(requestHeaderKey))
          // method type-specific filter
          .filtered[ByeFilter]
          .withRetryForClassifier(ByeResponseClassification)
          .service
      )
      // global filter
      .filtered(new MethodLoggingTypeAgnosticFilter())
  }

  private[this] val ByeResponseClassification: ResponseClassifier =
    ResponseClassifier.named("ByeMethodCustomResponseClassification") {
      case ReqRep(_, Return(result)) if result == "ERROR" => ResponseClass.RetryableFailure
      case ReqRep(_, Return(_)) => ResponseClass.Success
      case ReqRep(_, Throw(InvalidOperation(_))) => ResponseClass.RetryableFailure
      case ReqRep(_, Throw(NonFatal(_))) => ResponseClass.RetryableFailure
      case ReqRep(_, Throw(_)) => ResponseClass.NonRetryableFailure
    }
}
