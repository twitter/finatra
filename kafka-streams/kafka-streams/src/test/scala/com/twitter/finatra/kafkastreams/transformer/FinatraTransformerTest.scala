package com.twitter.finatra.kafkastreams.transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.{InMemoryStatsReceiver, NullStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.finatra.kafkastreams.transformer.stores.CachingKeyValueStores
import com.twitter.finatra.kafkastreams.transformer.watermarks.Watermark
import com.twitter.finatra.kafkastreams.test.KafkaTestUtil
import com.twitter.inject.{InMemoryStatsReceiverUtility, Test}
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.processor._
import org.apache.kafka.streams.processor.internals._
import org.apache.kafka.streams.state.internals.{FinatraStores, WrappedStateStore}
import org.apache.kafka.test.{InternalMockProcessorContext, MockProcessorNode, TestUtils}
import org.mockito.{ArgumentMatcher, ArgumentMatchers, Mockito}

class FinatraTransformerTest extends Test with com.twitter.util.mock.Mockito {
  val firstMessageTimestamp = 100000
  val firstKey = "key1"
  val firstValue = "value1"

  val secondMessageTimestamp = 200000
  val secondKey = "key2"
  val secondValue = "value2"

  test("watermark processing when forwarding from onMessage") {
    val transformer =
      new FinatraTransformer[String, String, String, String](NullStatsReceiver) {
        override def onMessage(messageTime: Time, key: String, value: String): Unit = {
          forward(key, value, watermark.timeMillis)
        }
      }

    val context = mock[ProcessorContext]
    context.taskId() returns new TaskId(0, 0)
    context.timestamp returns firstMessageTimestamp

    transformer.init(context)
    transformer.transform(firstKey, firstValue)
    transformer.watermark should be(Watermark(firstMessageTimestamp - 1))
    assertForwardedMessage(context, firstKey, firstValue, firstMessageTimestamp)

    context.timestamp returns secondMessageTimestamp
    transformer.transform(secondKey, secondValue)
    transformer.watermark should be(Watermark(firstMessageTimestamp - 1))
    assertForwardedMessage(context, secondKey, secondValue, firstMessageTimestamp)

    transformer.onFlush()
    transformer.watermark should be(Watermark(secondMessageTimestamp - 1))
  }

  test("watermark processing when forwarding from caching flush listener") {
    val transformer =
      new FinatraTransformer[String, String, String, String](NullStatsReceiver)
        with CachingKeyValueStores[String, String, String, String] {
        private val cache =
          getCachingKeyValueStore[String, String]("mystore", onFlushedEntry)

        override def commitInterval: Duration = 1.second

        override def onMessage(messageTime: Time, key: String, value: String): Unit = {
          cache.put(key, value)
        }

        private def onFlushedEntry(store: String, key: String, value: String): Unit = {
          forward(key = key, value = value, timestamp = watermark.timeMillis)
        }
      }

    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val statUtils = new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)

    val context = Mockito.spy(new CachingFinatraMockProcessorContext(inMemoryStatsReceiver))
    transformer.init(context)

    context.setTime(firstMessageTimestamp)
    transformer.transform(firstKey, firstValue)

    context.setTime(secondMessageTimestamp)
    transformer.transform(secondKey, secondValue)
    statUtils.gauges.assert("stores/mystore/numCacheEntries", 2.0f)

    transformer.onFlush()
    assertForwardedMessage(context, firstKey, firstValue, secondMessageTimestamp)
    assertForwardedMessage(context, secondKey, secondValue, secondMessageTimestamp)
    statUtils.gauges.assert("stores/mystore/numCacheEntries", 0.0f)
  }

  private def assertForwardedMessage(
    context: ProcessorContext,
    firstKey: String,
    firstValue: String,
    firstMessageTimestamp: Int
  ): Unit = {
    org.mockito.Mockito
      .verify(context)
      .forward(eqTo(firstKey), eqTo(firstValue), matchTo(firstMessageTimestamp - 1))
  }

  private def matchTo(expectedTimestamp: Int): To = {
    ArgumentMatchers.argThat(new ArgumentMatcher[To] {
      override def matches(to: To): Boolean = {
        val toInternal = new ToInternal
        toInternal.update(to)
        toInternal.timestamp() == expectedTimestamp
      }
    })
  }

  val config: KafkaStreamsConfig = new KafkaStreamsConfig()
    .commitInterval(Duration.Top)
    .applicationId("test-app")
    .bootstrapServers("127.0.0.1:1000")

  class CachingFinatraMockProcessorContext(statsReceiver: StatsReceiver)
      extends InternalMockProcessorContext(
        TestUtils.tempDirectory,
        new StreamsConfig(config.properties)) {

    override def currentNode(): ProcessorNode[_, _] = {
      new MockProcessorNode()
    }

    override def schedule(
      interval: Long,
      `type`: PunctuationType,
      callback: Punctuator
    ): Cancellable = {
      new Cancellable {
        override def cancel(): Unit = {
          //no-op
        }
      }
    }
    override def getStateStore(name: String): StateStore = {
      val storeBuilder = FinatraStores
        .keyValueStoreBuilder(
          statsReceiver,
          FinatraStores.persistentKeyValueStore(name),
          Serdes.String(),
          Serdes.String()
        )
        .withCachingEnabled()

      val store = storeBuilder.build
      store.init(this, store)

      new WrappedStateStore(store) {}
    }

    override def recordCollector(): RecordCollector = {
      KafkaTestUtil.createNoopRecord()
    }

    override def forward[K, V](key: K, value: V, to: To): Unit = {}
  }

}
