package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftTest}
import com.twitter.inject.Test
import com.twitter.inject.server.PortUtils
import com.twitter.snakeCase.thriftscala.{
  ConversationEvent,
  EnqueableEvents,
  EnqueueEventRequest,
  NotificationEvent,
  SnakeCaseService
}
import scala.util.Random

class MultiServerDarkTrafficFilterFeatureTest extends Test with ThriftTest {

  private[this] val darkSnakeCaseThriftServer =
    new EmbeddedThriftServer(new SnakeCaseThriftServer, disableTestLogging = true)
  private[this] val liveSnakeCaseThriftServer = new EmbeddedThriftServer(
    new SnakeCaseThriftServer,
    flags = Map(
      "thrift.dark.service.dest" -> s"/$$/inet/${PortUtils.loopbackAddress}/${darkSnakeCaseThriftServer
        .thriftPort()}",
      "thrift.dark.service.clientId" -> "client123"
    ),
    disableTestLogging = true
  )

  private[this] lazy val client123: SnakeCaseService.MethodPerEndpoint =
    liveSnakeCaseThriftServer
      .thriftClient[SnakeCaseService.MethodPerEndpoint](clientId = "client123")

  override protected def beforeEach(): Unit = {
    darkSnakeCaseThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
    liveSnakeCaseThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
  }

  override def afterAll(): Unit = {
    await(client123.asClosable.close())
    darkSnakeCaseThriftServer.close()
    liveSnakeCaseThriftServer.close()
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
    liveSnakeCaseThriftServer.inMemoryStats.counters
      .assert("per_method_stats/enqueue_event/success", 1)

    // darkTrafficFilter stats
    liveSnakeCaseThriftServer.inMemoryStats.counters.assert("dark_traffic_filter/forwarded", 1)
    liveSnakeCaseThriftServer.inMemoryStats.counters.get("dark_traffic_filter/skipped") should be(
      None)

    darkSnakeCaseThriftServer
      .assertHealthy() // give a chance for the stat to be recorded on the dark service
    // "dark" service stats
    darkSnakeCaseThriftServer.inMemoryStats.counters
      .waitFor("per_method_stats/enqueue_event/success", 1L)
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
    liveSnakeCaseThriftServer.inMemoryStats.counters
      .assert("per_method_stats/dequeue_event/success", 1)
    // darkTrafficFilter stats
    liveSnakeCaseThriftServer.inMemoryStats.counters.get("dark_traffic_filter/forwarded") should be(
      None)
    liveSnakeCaseThriftServer.inMemoryStats.counters.assert("dark_traffic_filter/skipped", 1)

    // "dark" service stats
    // no invocations on the doEverythingThriftServer1 as nothing is forwarded
    darkSnakeCaseThriftServer.inMemoryStats.counters
      .get("per_method_stats/dequeue_event/success") should be(None)
  }
}
