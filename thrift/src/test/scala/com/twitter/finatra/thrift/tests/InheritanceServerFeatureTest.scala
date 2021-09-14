package com.twitter.finatra.thrift.tests

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.inheritance.InheritanceServer
import com.twitter.inject.server.FeatureTest
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.util.Await

class InheritanceServerFeatureTest extends FeatureTest {

  protected val server = new EmbeddedThriftServer(new InheritanceServer, disableTestLogging = true)

  val client123 = server.thriftClient[ServiceB.MethodPerEndpoint](clientId = "client123")

  test("ServiceB#ping") {
    Await.result(client123.ping(), 2.seconds) should equal("pong")
  }

  test("ServiceB#echo") {
    val msg = "Hello, world!"
    Await.result(client123.echo(msg), 2.seconds) should equal(msg)
  }
}
