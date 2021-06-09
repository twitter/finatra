package com.twitter.inject.server

import com.twitter.inject.Test

class PortsTest extends Test {

  private[this] val server: Ports =
    new Ports {
      override def httpExternalPort: Some[Int] = Some(9999)
      override def httpsExternalPort: Some[Int] = Some(4443)
      override def thriftPort: Some[Int] = Some(9991)
    }

  override protected def afterAll(): Unit = {
    server.close()
    super.afterAll()
  }

  test("Ports#resolve httpExternalPort") {
    server.httpExternalPort shouldBe Some(9999)
  }

  test("Ports#resolve httpsExternalPort") {
    server.httpsExternalPort shouldBe Some(4443)
  }

  test("Ports#resolve thriftPort") {
    server.thriftPort shouldBe Some(9991)
  }
}
