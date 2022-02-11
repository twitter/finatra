package com.twitter.inject.thrift.integration.basic

import com.twitter.finagle.thrift.ClientId
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Singleton
class MyEchoService extends EchoService.MethodPerEndpoint with Logging {
  private val timesToEcho = new AtomicInteger(1)

  /* Public */
  override def echo(msg: String): Future[String] = {
    info("echo " + msg)
    assertClientId("echo-http-service")
    Future.value(msg * timesToEcho.get)
  }
  override def setTimesToEcho(times: Int): Future[Int] = {
    info("setTimesToEcho " + times)
    assertClientId("echo-http-service")
    timesToEcho.set(times) //mutation
    Future(times)
  }

  /* Private */
  private def assertClientId(name: String): Unit = {
    assert(ClientId.current.contains(ClientId(name)), "Invalid Client ID: " + ClientId.current)
  }
}
