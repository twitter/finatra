package com.twitter.calculator

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

class CalculatorServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedThriftServer(new CalculatorServer)

  val client = server.thriftClient[Calculator[Future]](clientId = "client123")

  test("client test") {
    await(client.increment(1)) should equal(2)
    await(client.addNumbers(1, 2)) should equal(3)
    await(client.addStrings("1", "2")) should equal("3")
  }

}
