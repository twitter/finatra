package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftTest}
import com.twitter.inject.Test
import com.twitter.snakeCase.thriftscala.{
  ConversationEvent,
  EnqueableEvents,
  EnqueueEventRequest,
  ExtendedSnakeCaseService,
  NotificationEvent
}
import scala.util.Random

abstract class AbstractExtendedMultiServerDarkTrafficFilterFeatureTest(
  darkExtendedSnakeCaseThriftServer: EmbeddedThriftServer,
  liveExtendedSnakeCaseThriftServer: EmbeddedThriftServer,
  clientId: String)
    extends Test
    with ThriftTest {

  private[this] lazy val client123: ExtendedSnakeCaseService.MethodPerEndpoint =
    liveExtendedSnakeCaseThriftServer
      .thriftClient[ExtendedSnakeCaseService.MethodPerEndpoint](clientId = clientId)

  override protected def beforeEach(): Unit = {
    darkExtendedSnakeCaseThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
    liveExtendedSnakeCaseThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
  }

  override def afterAll(): Unit = {
    await(client123.asClosable.close())
    darkExtendedSnakeCaseThriftServer.close()
    liveExtendedSnakeCaseThriftServer.close()
    super.afterAll()
  }

  test("enqueue_event is forwarded") {
    val request =
      EnqueueEventRequest(
        EnqueableEvents.NotificationEvent(
          new NotificationEvent.Immutable(highPriority = true, sourceEventName = "web")
        )
      )
    await(client123.enqueueEvent(request)) should equal(true)

    // service stats
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .assert("per_method_stats/enqueue_event/success", 1)

    // darkTrafficFilter stats
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .assert("dark_traffic_filter/forwarded", 1)
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .get("dark_traffic_filter/skipped") should be(None)

    darkExtendedSnakeCaseThriftServer
      .assertHealthy() // give a chance for the stat to be recorded on the dark service
    // "dark" service stats
    darkExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .waitFor("per_method_stats/enqueue_event/success", 1)
  }

  test("dequeue_event is not forwarded") {
    val request = EnqueueEventRequest(
      EnqueableEvents.ConversationEvent(
        new ConversationEvent.Immutable(
          conversationId = Random.nextLong(),
          tweetId = Random.nextLong(),
          userId = Random.nextLong(),
          subscribedUserIds = Seq(Random.nextLong()),
          eventTimeMs = System.currentTimeMillis()
        )))

    await(client123.dequeueEvent(request)) should equal(true)

    // service stats
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .assert("per_method_stats/dequeue_event/success", 1)
    // darkTrafficFilter stats
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .get("dark_traffic_filter/forwarded") should be(None)
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .assert("dark_traffic_filter/skipped", 1)

    // "dark" service stats
    // no invocations on the doEverythingThriftServer1 as nothing is forwarded
    darkExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .get("per_method_stats/dequeue_event/success") should be(None)
  }

  test("additional_event is forwarded") {
    val request =
      EnqueueEventRequest(
        EnqueableEvents.NotificationEvent(
          new NotificationEvent.Immutable(highPriority = true, sourceEventName = "web")
        )
      )
    await(client123.additionalEvent(request)) should equal(true)

    // service stats
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .assert("per_method_stats/additional_event/success", 1)

    // darkTrafficFilter stats
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .assert("dark_traffic_filter/forwarded", 1)
    liveExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .get("dark_traffic_filter/skipped") should be(None)

    darkExtendedSnakeCaseThriftServer
      .assertHealthy() // give a chance for the stat to be recorded on the dark service
    // "dark" service stats
    darkExtendedSnakeCaseThriftServer.inMemoryStats.counters
      .waitFor("per_method_stats/additional_event/success", 1)
  }
}
