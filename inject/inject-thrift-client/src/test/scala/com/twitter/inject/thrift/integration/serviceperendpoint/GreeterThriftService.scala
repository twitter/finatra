package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.{ByeOperation, ByeResponse, Greeter, InvalidOperation}
import com.twitter.inject.Logging
import com.twitter.inject.thrift.integration.AbstractThriftService
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger

class GreeterThriftService(
  clientId: String
) extends AbstractThriftService
  with Greeter.ServicePerEndpoint
  with Logging {

  private val hiNumCalled = new AtomicInteger(0)
  private val helloNumCalled = new AtomicInteger(0)
  private val byeNumCalled = new AtomicInteger(0)

  def hi: Service[Greeter.Hi.Args, Greeter.Hi.SuccessType] = Service.mk { args: Greeter.Hi.Args =>
    val numCalled = hiNumCalled.incrementAndGet()

    val name = args.name
    info(s"Hi called with message: " + name)
    assertClientId(clientId)

    if (numCalled == 1)
      Future.exception(new IllegalArgumentException)
    else if (numCalled == 2)
      Future.exception(new InvalidOperation(what = 123, why = "whoops"))
    else if (numCalled == 3)
      Future.value("ERROR")
    else
      Future.value(s"Hi $name")
  }

  def hello: Service[Greeter.Hello.Args, Greeter.Hello.SuccessType] = Service.mk {
    args: Greeter.Hello.Args =>
      val numCalled = helloNumCalled.incrementAndGet()

      val name = args.name
      info(s"Hello called with message: " + name)
      assertClientId(clientId)

      if (numCalled == 1)
        Future.exception(new IllegalArgumentException)
      else if (numCalled == 2)
        Future.exception(new InvalidOperation(what = 123, why = "whoops"))
      else
        Future.value(s"Hello $name")
  }

  def bye: Service[Greeter.Bye.Args, Greeter.Bye.SuccessType] = Service.mk {
    args: Greeter.Bye.Args =>
      val numCalled = byeNumCalled.incrementAndGet()
      assertClientId(clientId)

      if (numCalled == 1)
        Future.exception(new NullPointerException)
      else if (numCalled == 2)
        Future.exception(new ByeOperation(code = 456))
      else
        Future.value(ByeResponse(code = 123, s"Bye ${args.name} of ${args.age} years!"))
  }
}
