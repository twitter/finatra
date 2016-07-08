package com.twitter.finatra.multiserver.Add1HttpServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder._
import com.twitter.finagle.Filter
import com.twitter.inject.thrift.filters.ThriftClientFilterBuilder
import com.twitter.inject.thrift.modules.FilteredThriftClientModule
import com.twitter.util.Future

object AdderThriftClientModule
  extends FilteredThriftClientModule[Adder[Future], Adder.ServiceIface] {

  override val label = "adder-thrift"
  override val dest = "flag!adder-thrift-server"

  override def filterServiceIface(
    serviceIface: ServiceIface,
    filter: ThriftClientFilterBuilder): ServiceIface = {

    serviceIface.copy(
      add1 = filter.method(Add1)
        .exceptionFilter(Filter.identity[Add1.Args, Add1.Result]) // Example of replacing the default exception filter
        .timeout(3.minutes)
        .exponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.add1),
      add1String = filter.method(Add1String)
        .timeout(3.minutes)
        .exponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.add1String),
      add1Slowly = filter.method(Add1Slowly)
        .timeout(3.minutes)
        .exponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          requestTimeout = 1.millis, // We purposely set a very small timeout so that we can test handling IndividualRequestTimeoutException
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.add1Slowly),
      add1AlwaysError = filter.method(Add1AlwaysError)
        .timeout(3.minutes)
        .exponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.add1AlwaysError))
  }
}
