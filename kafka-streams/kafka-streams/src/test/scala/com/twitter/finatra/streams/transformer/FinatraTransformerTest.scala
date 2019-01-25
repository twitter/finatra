package com.twitter.finatra.streams.transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.finatra.streams.transformer.domain.{Time, Watermark}
import com.twitter.inject.Test
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.processor._
import org.apache.kafka.streams.processor.internals.{RecordCollector, ToInternal}
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.test.{InternalMockProcessorContext, NoOpRecordCollector, TestUtils}
import org.hamcrest.{BaseMatcher, Description}
import org.mockito.{Matchers, Mockito}

class FinatraTransformerTest extends Test with com.twitter.inject.Mockito {
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

    val context = smartMock[ProcessorContext]
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
        private val cache = getCachingKeyValueStore[String, String]("mystore")

        override def statsReceiver: StatsReceiver = NullStatsReceiver
        override def commitInterval: Duration = 1.second

        override def onInit(): Unit = {
          super.onInit()
          cache.registerFlushListener(onFlushed)
        }

        override def onMessage(messageTime: Time, key: String, value: String): Unit = {
          cache.put(key, value)
        }

        private def onFlushed(key: String, value: String): Unit = {
          forward(key = key, value = value, timestamp = watermark.timeMillis)
        }
      }

    val context = Mockito.spy(new FinatraMockProcessorContext)
    transformer.init(context)

    context.setTime(firstMessageTimestamp)
    transformer.transform(firstKey, firstValue)

    context.setTime(secondMessageTimestamp)
    transformer.transform(secondKey, secondValue)

    transformer.onFlush()
    assertForwardedMessage(context, firstKey, firstValue, secondMessageTimestamp)
    assertForwardedMessage(context, secondKey, secondValue, secondMessageTimestamp)
  }

  private def assertForwardedMessage(
    context: ProcessorContext,
    firstKey: String,
    firstValue: String,
    firstMessageTimestamp: Int
  ): Unit = {
    org.mockito.Mockito
      .verify(context)
      .forward(meq(firstKey), meq(firstValue), matchTo(firstMessageTimestamp - 1))
  }

  private def matchTo(expectedTimestamp: Int): To = {
    Matchers.argThat(new BaseMatcher[To] {
      override def matches(to: scala.Any): Boolean = {
        val toInternal = new ToInternal
        toInternal.update(to.asInstanceOf[To])
        toInternal.timestamp() == expectedTimestamp
      }

      override def describeTo(description: Description): Unit = {
        description.appendText(s"To(timestamp = $expectedTimestamp)")
      }
    })
  }

  val config = new KafkaStreamsConfig()
    .commitInterval(Duration.Top)
    .applicationId("test-app")
    .bootstrapServers("127.0.0.1:1000")

  class FinatraMockProcessorContext
      extends InternalMockProcessorContext(
        TestUtils.tempDirectory,
        new StreamsConfig(config.properties)) {

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
      val storeBuilder = Stores
        .keyValueStoreBuilder(
          Stores.persistentKeyValueStore(name),
          Serdes.String(),
          Serdes.String()
        )

      val store = storeBuilder.build
      store.init(this, store)
      store
    }

    override def recordCollector(): RecordCollector = {
      new NoOpRecordCollector
    }

    override def forward[K, V](key: K, value: V, to: To): Unit = {}
  }

}
