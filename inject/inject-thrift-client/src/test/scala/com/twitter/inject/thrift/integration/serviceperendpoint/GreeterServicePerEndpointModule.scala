package com.twitter.inject.thrift.integration.serviceperendpoint

import com.google.inject.Module
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.greeter.thriftscala.{Greeter, InvalidOperation}
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.integration.filters.{HiLoggingTypeAgnosticFilter, MethodLoggingTypeAgnosticFilter}
import com.twitter.inject.thrift.modules.{ServicePerEndpointModule, ThriftClientIdModule}
import com.twitter.util.{Return, Throw}
import scala.util.control.NonFatal

object GreeterServicePerEndpointModule
  extends ServicePerEndpointModule[Greeter.ServicePerEndpoint, Greeter.MethodPerEndpoint] {

  override val modules: Seq[Module] = Seq(ThriftClientIdModule)

  override val dest = "flag!greeter-thrift-service"
  override val label = "greeter-thrift-client"

  override def configureServicePerEndpoint(
    builder: ThriftMethodBuilderFactory[Greeter.ServicePerEndpoint],
    servicePerEndpoint: Greeter.ServicePerEndpoint
  ): Greeter.ServicePerEndpoint = {

    servicePerEndpoint
      .withHi(
        builder.method[Greeter.Hi.Args, Greeter.Hi.SuccessType](Greeter.Hi)
          // method type-agnostic filter
          .withAgnosticFilter[HiLoggingTypeAgnosticFilter]
          .withRetryForClassifier(PossiblyRetryableExceptions)
          .service)
      .withHello(
        builder.method(Greeter.Hello)
          // method type-specific filter
          .filtered(new HelloFilter)
          .withRetryForClassifier(ByeResponseClassification)
          .service)
      .withBye(
        builder.method[Bye.Args, Bye.SuccessType](Greeter.Bye)
          // method type-specific filter
          .filtered[ByeFilter]
          .withRetryForClassifier(PossiblyRetryableExceptions)
          .service)
      // global (type-agnostic) filter
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
