package com.twitter.finatra.example

import com.twitter.mock.Mockito
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}
import org.scalatest.concurrent.Eventually

// c.t.inject.server.FeatureTest will close the server
class ExampleTwitterServerFeatureTest extends FeatureTest with Mockito with Eventually {
  private val testQueue = new TestQueue

  val server: EmbeddedTwitterServer = new EmbeddedTwitterServer(
    twitterServer = new ExampleTwitterServer,
    disableTestLogging = true
  ).bind[Queue].toInstance(testQueue)

  test("Queue test") {
    server.start()
    server.assertHealthy()

    val subscriber = server.injector.instance[Subscriber]
    testQueue.addCounter.get should equal(5)
  }
}
