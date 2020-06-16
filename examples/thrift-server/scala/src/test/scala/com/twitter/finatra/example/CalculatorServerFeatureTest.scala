package com.twitter.finatra.example

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest

class CalculatorServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedThriftServer(
    new CalculatorServer,
    disableTestLogging = true
  )

  val client = server.thriftClient[Calculator.MethodPerEndpoint](clientId = "client123")

  test("client test") {
    await(client.increment(1)) should equal(2)
    await(client.addNumbers(1, 2)) should equal(3)
    await(client.addStrings("1", "2")) should equal("3")
  }

}
