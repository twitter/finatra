package com.twitter.inject.server

import com.twitter.finagle.ListeningServer
import com.twitter.inject.Test
import com.twitter.util.Awaitable.CanAwait
import com.twitter.util.{Time, Duration, Future}
import java.net.InetSocketAddress

class PortUtilsTest extends Test {

  private[this] val server: ListeningServer = new ListeningServer {
    override def boundAddress = new InetSocketAddress(9999)
    override protected def closeServer(deadline: Time): Future[Unit] = Future.Done
    override def result(timeout: Duration)(implicit permit: CanAwait) = ???
    override def isReady(implicit permit: CanAwait) = ???
    override def ready(timeout: Duration)(implicit permit: CanAwait) = ???
  }

  override protected def afterAll(): Unit = {
    server.close()
    super.afterAll()
  }

  test("PortUtils#ephemeralLoopback") {
    PortUtils.ephemeralLoopback.isEmpty should be(false)
  }

  test("PortUtils#getPort for ListeningServer") {
    PortUtils.getPort(server) should be(9999)
  }

  test("PortUtils#getSocketAddress for ListeningServer") {
    PortUtils.getSocketAddress(server) should equal(new InetSocketAddress(9999))
  }

  test("PortUtils#getPort for SocketAddress") {
    PortUtils.getPort(new InetSocketAddress(9999)) should be(9999)
  }

  test("PortUtils#loopbackAddressForPort") {
    PortUtils.loopbackAddressForPort(9999) should be(s"${PortUtils.loopbackAddress}:9999")
  }

  test("PortUtils#parseAddr for ListeningServer") {
    PortUtils.parseAddr(":9999") should equal(new InetSocketAddress(9999))
  }
}
