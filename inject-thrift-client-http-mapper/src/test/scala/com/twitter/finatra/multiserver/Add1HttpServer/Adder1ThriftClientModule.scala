package com.twitter.finatra.multiserver.Add1HttpServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder.{Add1, Add1String, ServiceIface}
import com.twitter.finagle.{Filter, Thrift}
import com.twitter.inject.thrift.{FilterBuilder, FilteredThriftClientModule, ThriftClientIdModule}
import com.twitter.util.Future

object Adder1ThriftClientModule extends FilteredThriftClientModule[Adder[Future], Adder.ServiceIface] {
  override val modules = Seq(ThriftClientIdModule)
  override val label = "add1-thrift"
  override val dest = "flag!add1-thrift-server"

  override def filterServiceIface(
    serviceIface: ServiceIface,
    filters: FilterBuilder): ServiceIface = {

    serviceIface.copy(
      add1 = filters.method(Add1)
        .exceptionFilter(Filter.identity[Add1.Args, Add1.Result]) // Example of replacing the default exception filter
        .timeout(3.minutes)
        .exponentialRetry(
          shouldRetryResponse = NonCancelledExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.add1),
      add1String = filters.method(Add1String)
        .timeout(3.minutes)
        .exponentialRetry(
          shouldRetryResponse = NonCancelledExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.add1String))
  }
}
