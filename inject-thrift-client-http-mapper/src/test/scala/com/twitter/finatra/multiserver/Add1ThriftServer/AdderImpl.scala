package com.twitter.finatra.multiserver.Add1ThriftServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.CancelledRequestException
import com.twitter.finatra.thrift.thriftscala.{ServerError, ServerErrorCause}
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Singleton
class AdderImpl extends Adder[Future] {

  private val add1MethodCalls = new AtomicInteger(0)
  private val addStringsMethodCalls = new AtomicInteger(0)

  override def add1(num: Int): Future[Int] = {
    val numCalled = add1MethodCalls.incrementAndGet()

    if (numCalled == 1)
      Future.exception(new IllegalArgumentException)
    else if (numCalled == 2)
      Future.exception(new ServerError(ServerErrorCause.InternalServerError, "oops"))
    else
      Future.value(num + 1)
  }

  override def add1String(num: String): Future[String] = {
    val numCalled = addStringsMethodCalls.incrementAndGet()

    if (numCalled == 1)
      Future.exception(new IllegalArgumentException)
    else if (numCalled == 2)
      Future.exception(new CancelledRequestException)
    else if (numCalled == 3)
      Future.exception(new NullPointerException)
    else if (numCalled < 6)
      Future.exception(new ServerError(ServerErrorCause.InternalServerError, "oops " + numCalled))
    else
      Future.value((num.toInt + 1).toString)
  }
}
