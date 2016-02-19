package com.twitter.finatra.multiserver.AdderThriftServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder._
import com.twitter.finagle.CancelledRequestException
import com.twitter.finatra.thrift.Controller
import com.twitter.finatra.thrift.thriftscala.{ServerError, ServerErrorCause}
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Singleton
class AdderThriftController
  extends Controller
  with Adder.BaseServiceIface {

  private val add1MethodCalls = new AtomicInteger(0)
  private val addStringsMethodCalls = new AtomicInteger(0)

  override val add1 = handle(Add1) { args: Add1.Args =>
    val numCalled = add1MethodCalls.incrementAndGet()

    if (numCalled == 1)
      Future.exception(new IllegalArgumentException)
    else if (numCalled == 2)
      Future.exception(new ServerError(ServerErrorCause.InternalServerError, "oops"))
    else
      Future.value(args.num + 1)
  }

  override val add1String = handle(Add1String) { args: Add1String.Args =>
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
      Future((args.num.toInt + 1).toString)
  }

  override val add1Slowly = handle(Add1Slowly) { args: Add1Slowly.Args =>
    Thread.sleep(5000)
    Future((args.num.toInt + 1).toString)
  }

  override val add1AlwaysError = handle(Add1AlwaysError) { args: Add1AlwaysError.Args =>
    Future.exception(new RuntimeException("oops"))
  }
}
