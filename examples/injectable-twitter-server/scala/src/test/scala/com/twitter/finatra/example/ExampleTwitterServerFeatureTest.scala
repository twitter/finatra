package com.twitter.finatra.example

import com.twitter.conversions.DurationOps._
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}
import com.twitter.util.Await

// c.t.inject.server.FeatureTest will close the server
class ExampleTwitterServerFeatureTest extends FeatureTest {
  private val SubscriberMaxRead: Int = 3
  private val testQueue: TestQueue = new TestQueue

  val server: EmbeddedTwitterServer = new EmbeddedTwitterServer(
    twitterServer = new ExampleTwitterServer,
    flags = Map("subscriber.max.read" -> SubscriberMaxRead.toString),
    disableTestLogging = true
  ).bind[Queue].toInstance(testQueue)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    server.start()
    server.assertHealthy()
  }

  test("Queue test") {
    testQueue.addCounter.get should equal(5)
    // wait for underlying server to finish main method which should happen once Subscriber returns
    // Subscriber should exit once it hits the SubscriberMaxRead value.
    Await.result(server.mainResult, 10.seconds)
    val subscriber = server.injector.instance[Subscriber]
    subscriber.readCount should equal(SubscriberMaxRead)
  }
}
