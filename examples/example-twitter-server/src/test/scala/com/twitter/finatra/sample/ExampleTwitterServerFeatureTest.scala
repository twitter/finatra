package com.twitter.finatra.sample

import com.twitter.inject.Mockito
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}

class ExampleTwitterServerFeatureTest extends FeatureTest with Mockito {
  private val queue = new TestQueue

  val server = new EmbeddedTwitterServer(
    twitterServer = new ExampleTwitterServer,
    disableTestLogging = true
  ).bind[Queue].toInstance(queue)

  test("Queue test") {
    server.start()
    server.assertHealthy()

    val subscriber = server.injector.instance[Subscriber]
    queue.addCounter.get should equal(subscriber.numRead.get)
  }
}


