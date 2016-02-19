package com.twitter.inject.thrift

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{CancelledRequestException, Failure, CancelledConnectionException}
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.inject.Test
import com.twitter.inject.thrift.filters.FilterBuilder
import com.twitter.inject.thrift.modules.FilteredThriftClientModule
import com.twitter.util.Future

class FilteredThriftClientModuleTest extends Test {

  val module = new FilteredThriftClientModule[Greeter[Future], Greeter.ServiceIface] {
    val label = ""
    val dest = ""
    def filterServiceIface(serviceIface: Greeter.ServiceIface,filters: FilterBuilder) = ???
  }

  "test NonCancelledExceptions matcher" in {
    assertIsCancellation(new CancelledRequestException)
    assertIsCancellation(new CancelledConnectionException(new Exception("cause")))
    assertIsCancellation(new ClientDiscardedRequestException("cause"))
    assertIsCancellation(Failure("int", Failure.Interrupted))
    assertIsCancellation(Failure.rejected("", new CancelledRequestException))
  }

  private def assertIsCancellation(e: Throwable) {
    module.isCancellation(e) should be(true)
  }
}
