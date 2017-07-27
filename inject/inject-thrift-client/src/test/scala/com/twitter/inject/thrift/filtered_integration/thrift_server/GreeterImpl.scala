package com.twitter.inject.thrift.filtered_integration.thrift_server

import com.twitter.finagle.thrift.ClientId
import com.twitter.greeter.thriftscala.{ByeOperation, ByeResponse, Greeter, InvalidOperation}
import com.twitter.inject.Logging
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Singleton
class GreeterImpl extends Greeter[Future] with Logging {

  private val hiNumCalled = new AtomicInteger(0)
  private val byeNumCalled = new AtomicInteger(0)

  /* Public */

  override def hi(name: String): Future[String] = {
    val numCalled = hiNumCalled.incrementAndGet()
    info(s"Hi called with message: " + name)
    assertClientId("greeter-http-service")

    if (numCalled == 1)
      Future.exception(new IllegalArgumentException)
    else if (numCalled == 2)
      Future.exception(new InvalidOperation(what = 123, why = "woops"))
    else if (numCalled == 3)
      Future.value("ERROR")
    else
      Future.value(s"Hi $name")
  }

  override def bye(name: String, age: Int): Future[ByeResponse] = {
    val numCalled = byeNumCalled.incrementAndGet()

    if (numCalled == 1)
      Future.exception(new NullPointerException)
    else if (numCalled == 2)
      Future.exception(new ByeOperation(code = 456))
    else
      Future.value(ByeResponse(code = 123, s"Bye $name of $age years!"))
  }

  /* Private */

  private def assertClientId(name: String): Unit = {
    assert(ClientId.current.exists(_ == ClientId(name)), "Invalid Client ID: " + ClientId.current)
  }
}
