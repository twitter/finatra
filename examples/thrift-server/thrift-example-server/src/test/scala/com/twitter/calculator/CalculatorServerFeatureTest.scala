package com.twitter.calculator

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

class CalculatorServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedThriftServer(new CalculatorServer)

  val client = server.thriftClient[Calculator[Future]](clientId = "client123")

  test("acceptlist#clients allowed") {
    await(client.increment(1)) should equal(2)
    await(client.addNumbers(1, 2)) should equal(3)
    await(client.addStrings("1", "2")) should equal("3")
  }

  test("denylist#clients blocked with UnknownClientIdException") {
    val clientWithUnknownId = server.thriftClient[Calculator[Future]](clientId = "unlisted-client")
    intercept[UnknownClientIdError] { 
      await(clientWithUnknownId.increment(2)) 
    }
  }

  test("clients#without a client-id blocked with NoClientIdException") {
    val clientWithoutId = server.thriftClient[Calculator[Future]]()
    intercept[NoClientIdError] { 
      await(clientWithoutId.increment(1)) 
    }
  }
}
