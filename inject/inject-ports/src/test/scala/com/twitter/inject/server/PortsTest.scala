package com.twitter.inject.server

import com.twitter.inject.Test

class PortsTest extends Test {

  lazy val server: Ports =
    new Ports {
      override def httpExternalPort = Some(9999)
      override def httpsExternalPort = Some(4443)
      override def thriftPort = Some(9991)
    }

  override protected def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      server.close()
    }
  }

  test("Ports#resolve") {
    server.httpExternalPort shouldBe Some(9999)
    server.httpsExternalPort shouldBe Some(4443)
    server.thriftPort shouldBe Some(9991)
  }
}
