package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.twitter.finagle.Service
import com.twitter.greeter.thriftscala.{ByeOperation, ByeResponse, Greeter, InvalidOperation}
import com.twitter.inject.Logging
import com.twitter.inject.thrift.integration.AbstractThriftService
import com.twitter.scrooge
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger

class GreeterThriftService(
  clientId: String,
  requestHeaderKey: String
) extends AbstractThriftService
  with Greeter.ReqRepServicePerEndpoint
  with Logging {

  private val hiNumCalled = new AtomicInteger(0)
  private val helloNumCalled = new AtomicInteger(0)
  private val byeNumCalled = new AtomicInteger(0)

  def hi: Service[scrooge.Request[Greeter.Hi.Args], scrooge.Response[Greeter.Hi.SuccessType]] =
    Service.mk { request: scrooge.Request[Greeter.Hi.Args] =>
      if (requestHeaderKey.nonEmpty) assert(request.headers.contains(requestHeaderKey))
      val numCalled = hiNumCalled.incrementAndGet()

      val name = request.args.name
      info(s"Hi called with message: " + name)
      assertClientId(clientId)

      if (numCalled == 1)
        Future.exception(new IllegalArgumentException)
      else if (numCalled == 2)
        Future.exception(new InvalidOperation(what = 123, why = "whoops"))
      else if (numCalled == 3)
        Future.value(scrooge.Response("ERROR"))
      else
        Future.value(scrooge.Response(s"Hi $name"))
    }

  def hello: Service[
    scrooge.Request[Greeter.Hello.Args],
    scrooge.Response[Greeter.Hello.SuccessType]] =
    Service.mk { request: scrooge.Request[Greeter.Hello.Args] =>
      if (requestHeaderKey.nonEmpty) assert(request.headers.contains(requestHeaderKey))
      val numCalled = helloNumCalled.incrementAndGet()

      val name = request.args.name
      info(s"Hello called with message: " + name)
      assertClientId(clientId)

      if (numCalled == 1)
        Future.exception(new IllegalArgumentException)
      else if (numCalled == 2)
        Future.exception(new InvalidOperation(what = 123, why = "whoops"))
      else
        Future.value(
          scrooge.Response(s"Hello $name")
            .setHeader("com.twitter.greeter.hello", "bar"))
    }

  def bye: Service[scrooge.Request[Greeter.Bye.Args], scrooge.Response[Greeter.Bye.SuccessType]] =
    Service.mk { request: scrooge.Request[Greeter.Bye.Args] =>
      if (requestHeaderKey.nonEmpty) assert(request.headers.contains(requestHeaderKey))
      val numCalled = byeNumCalled.incrementAndGet()
      assertClientId(clientId)

      if (numCalled == 1)
        Future.exception(new NullPointerException)
      else if (numCalled == 2)
        Future.exception(new ByeOperation(code = 456))
      else
        Future.value(
          scrooge.Response(
            ByeResponse(code = 123, s"Bye ${request.args.name} of ${request.args.age} years!")
          )
        )
    }
}
