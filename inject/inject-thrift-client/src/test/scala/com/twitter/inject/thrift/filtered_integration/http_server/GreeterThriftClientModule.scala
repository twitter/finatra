package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.greeter.thriftscala.Greeter.{Bye, Hi}
import com.twitter.greeter.thriftscala.{Greeter, InvalidOperation}
import com.twitter.inject.thrift.filters.ThriftClientFilterBuilder
import com.twitter.inject.thrift.modules.FilteredThriftClientModule
import com.twitter.util._

object GreeterThriftClientModule
  extends FilteredThriftClientModule[Greeter[Future], Greeter.ServiceIface] {

  override val label = "greeter-thrift-client"
  override val dest = "flag!greeter-thrift-service"
  override val sessionAcquisitionTimeout = 1.minute.toDuration

  override def filterServiceIface(
    serviceIface: Greeter.ServiceIface,
    filter: ThriftClientFilterBuilder) = {

    serviceIface.copy(
      hi = filter.method(Hi)
        .timeout(2.minutes)
        .constantRetry(
          requestTimeout = 1.minute,
          shouldRetryResponse = {
            case Return(Hi.Result(_, Some(e: InvalidOperation))) => true
            case Return(Hi.Result(Some(success), _)) => success == "ERROR"
            case Throw(NonFatal(_)) => true
          },
          start = 50.millis,
          retries = 3)
        .filter[HiLoggingThriftClientFilter]
        .andThen(serviceIface.hi),
      bye = filter.method(Bye)
        .exponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.bye))
  }
}
