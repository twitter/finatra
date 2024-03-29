package com.twitter.finatra.thrift.tests

import com.twitter.conversions.DurationOps._
import com.twitter.noninjection.thriftscala.NonInjectionService
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.noninjection.NonInjectionThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Await

/** Tests that we can successfully bring up and query a service without injection. */
class NonInjectionThriftServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(
    twitterServer = new NonInjectionThriftServer(),
    disableTestLogging = true)

  val client = server.thriftClient[NonInjectionService.MethodPerEndpoint](clientId = "client")

  test("success") {
    Await.result(client.echo("Hi"), 2.seconds) should equal("Hi")
  }
}
