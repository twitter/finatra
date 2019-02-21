package com.twitter.finatra.kafkastreams.integration.sampling

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.internal.utils.sampling.IndexedSampleKey
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.kafkastreams.transformer.utils.{IteratorImplicits, SamplingUtils}
import org.apache.kafka.streams.state.KeyValueStore
import org.joda.time.DateTime

class SamplingServerTopologyFeatureTest extends TopologyFeatureTest with IteratorImplicits {

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "sampling-server-prod-alice",
    server = new SamplingServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val tweetIdToImpressingUserId =
    topologyTester.topic(
      SamplingServer.tweetToImpressingUserTopic,
      ScalaSerdes.Long,
      ScalaSerdes.Long)

  private var countStore: KeyValueStore[Long, Long] = _

  private var sampleStore: KeyValueStore[IndexedSampleKey[Long], Long] = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    countStore = topologyTester.driver.getKeyValueStore[Long, Long](
      SamplingUtils.getNumCountsStoreName(SamplingServer.sampleName))
    sampleStore = topologyTester.driver.getKeyValueStore[IndexedSampleKey[Long], Long](
      SamplingUtils.getSampleStoreName(SamplingServer.sampleName))
  }

  test("test that a sample does what you want") {
    val tweetId = 1

    publishImpression(tweetId, userId = 1)
    countStore.get(tweetId) should equal(1)
    assertSampleSize(tweetId, 1)
    assertSampleEquals(tweetId, Set(1))

    publishImpression(tweetId, userId = 2)
    countStore.get(tweetId) should equal(2)
    assertSampleSize(tweetId, 2)
    assertSampleEquals(tweetId, Set(1, 2))

    publishImpression(tweetId, userId = 3)
    countStore.get(tweetId) should equal(3)
    assertSampleSize(tweetId, 3)
    assertSampleEquals(tweetId, Set(1, 2, 3))

    publishImpression(tweetId, userId = 4)
    countStore.get(tweetId) should equal(4)
    assertSampleSize(tweetId, 4)
    assertSampleEquals(tweetId, Set(1, 2, 3, 4))

    publishImpression(tweetId, userId = 5)
    countStore.get(tweetId) should equal(5)
    assertSampleSize(tweetId, 5)
    assertSampleEquals(tweetId, Set(1, 2, 3, 4, 5))

    publishImpression(tweetId, userId = 6)
    countStore.get(tweetId) should equal(6)
    assertSampleSize(tweetId, 5)

    // advance time and verify that the sample is thrown away.
    topologyTester.advanceWallClockTime(2.minutes)
    publishImpression(tweetId = 666, userId = 666)
    topologyTester.stats.assertCounter("kafka/stream/numExpired", 0)
    topologyTester.advanceWallClockTime(2.minutes)
    topologyTester.stats.assertCounter("kafka/stream/numExpired", 1)
    assert(countStore.get(tweetId) == 0)
    assertSampleSize(tweetId, 0)
  }

  private def assertSampleSize(tweetId: Int, expectedSize: Int): Unit = {
    val range =
      sampleStore.range(IndexedSampleKey(tweetId, 0), IndexedSampleKey(tweetId, Int.MaxValue))
    range.values.toSet.size should be(expectedSize)
  }

  private def assertSampleEquals(tweetId: Int, expectedSample: Set[Int]): Unit = {
    val range =
      sampleStore.range(IndexedSampleKey(tweetId, 0), IndexedSampleKey(tweetId, Int.MaxValue))
    range.values.toSet should be(expectedSample)
  }

  private def publishImpression(
    tweetId: Long,
    userId: Long,
    publishTime: DateTime = DateTime.now
  ): Unit = {
    tweetIdToImpressingUserId.pipeInput(tweetId, userId)
  }
}
