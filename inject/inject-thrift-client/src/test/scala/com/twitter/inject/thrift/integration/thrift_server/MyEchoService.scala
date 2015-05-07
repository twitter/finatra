package com.twitter.inject.thrift.integration.thrift_server

import com.twitter.inject.Logging
import com.twitter.test.thriftscala.{ClientError, ClientErrorCause, EchoService}
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Singleton
class MyEchoService
  extends EchoService[Future]
  with Logging {

  private val timesToEcho = new AtomicInteger(1)

  private val setTimesToEchoCalledCount = new AtomicInteger(0)
  private val echoMethodCalledCount = new AtomicInteger(0)

  /* Public */

  override def echo(msg: String): Future[String] = {
    info("echo " + msg)
    echoMethodCalledCount.incrementAndGet() match {
      case 1 =>
        Future.exception(new NullPointerException)
      case 2 =>
        Future.value("woops")
      case 3 =>
        Future.exception(ClientError(ClientErrorCause.RateLimited, "slow down"))
      case _ =>
        Future.value(
          msg * timesToEcho.get)
    }
  }

  override def setTimesToEcho(times: Int): Future[Int] = {
    info("setTimesToEcho " + times)
    setTimesToEchoCalledCount.incrementAndGet() match {
      case 1 =>
        Future.exception(new NoSuchMethodException)
      case 2 =>
        Future.value(-1)
      case 3 =>
        Future.exception(ClientError(ClientErrorCause.RequestTimeout, "slow"))
      case _ =>
        timesToEcho.set(times) //mutation
        Future(times)
    }
  }
}
