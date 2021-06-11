package com.twitter.finatra.kafkastreams.integration.config

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.{DefaultTopicConfig, FinatraRocksDBConfig, FinatraTransformerFlags, KafkaStreamsConfig, RocksDbFlags}
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, IteratorWithAutoCloseToSeq, TopologyFeatureTest, TopologyTesterTopic}
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.{Expire, Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.stores.PersistentTimers
import com.twitter.finatra.kafkastreams.transformer.stores.internal.Timer
import com.twitter.finatra.kafkastreams.transformer.utils.IteratorImplicits
import com.twitter.finatra.streams.transformer.internal.domain.TimerSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.internals.FinatraStores
import org.joda.time.DateTime

abstract class FinatraTransformerChangeLogConfigFeatureTest
  extends TopologyFeatureTest
    with IteratorImplicits
    with IteratorWithAutoCloseToSeq {

  val appId = "no-op"
  val sourceTopicName = "source"
  val sinkTopicName = "sink"
  val stateStoreName = "test-state-store"
  val timerStoreName = "test-timer-store"
  val stateStoreChangelogTopicName = s"$appId-$stateStoreName-changelog"
  val timerStoreChangelogTopicName = s"$appId-$timerStoreName-changelog"
  val startTime = new DateTime("1970-01-01T00:00:00.000Z")
  val expirationTimeMills: Long = 1.minutes.inMillis

  protected def configureStateStores(builder: StreamsBuilder): Unit = {}

  val kafkaStreamsTwitterServer: KafkaStreamsTwitterServer = new KafkaStreamsTwitterServer
    with RocksDbFlags
    with FinatraTransformerFlags {
    override val name: String = appId

    override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
      configureStateStores(builder)

      val finatraTransformerSupplier = () =>
        new FinatraTransformer[String, String, String, String](statsReceiver = streamsStatsReceiver)
          with PersistentTimers {
          private val stateStore = getKeyValueStore[String, String](stateStoreName)
          private val keyTimerStore = getPersistentTimerStore[String](
            timerStoreName,
            onEventTimer,
            PunctuationType.STREAM_TIME)

          private def onEventTimer(time: Time, metadata: TimerMetadata, key: String): Unit = {
            assert(metadata == Expire)
            stateStore.delete(key)
          }

          override def onMessage(messageTime: Time, key: String, value: String): Unit = {
            stateStore.put(key, value)
            keyTimerStore.addTimer(Time(expirationTimeMills), Expire, key)
            forward(key, value, watermark.timeMillis)
          }
        }

      builder.asScala
        .stream(sourceTopicName)(Consumed.`with`(Serdes.String, Serdes.String))
        .transform(finatraTransformerSupplier, stateStoreName, timerStoreName)
        .to(sinkTopicName)(Produced.`with`(Serdes.String, Serdes.String))
    }

    override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
      super.streamsProperties(config).rocksDbConfigSetter[FinatraRocksDBConfig]
    }
  }

  override protected val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = appId,
    server = kafkaStreamsTwitterServer,
    startingWallClockTime = startTime,
    finatraTransformer = true,
    flags = Map())

  val sourceTopic: TopologyTesterTopic[String, String] = topologyTester.topic(
    sourceTopicName,
    Serdes.String,
    Serdes.String
  )

  val sinkTopic: TopologyTesterTopic[String, String] = topologyTester.topic(
    sinkTopicName,
    Serdes.String,
    Serdes.String
  )

  val stateStoreChangelogTopic: TopologyTesterTopic[String, String] = topologyTester.topic(
    stateStoreChangelogTopicName,
    Serdes.String,
    Serdes.String
  )

  val timerStoreChangelogTopic: TopologyTesterTopic[Timer[String], Array[Byte]] =
    topologyTester.topic(
      timerStoreChangelogTopicName,
      TimerSerde(Serdes.String),
      Serdes.ByteArray()
    )

  def stateStoreTestProbe(): KeyValueStore[String, String] = topologyTester
    .driver
    .getKeyValueStore[String, String](stateStoreName)

  def timerStoreTestProbe(): KeyValueStore[Timer[String], Array[Byte]] = topologyTester
    .driver
    .getKeyValueStore[Timer[String], Array[Byte]](timerStoreName)

  override def beforeEach(): Unit = {
    super.beforeEach()
    topologyTester.reset()
  }
}

class WithLoggingEnabled extends FinatraTransformerChangeLogConfigFeatureTest {
  override def configureStateStores(builder: StreamsBuilder): Unit = {
    builder.addStateStore(
      FinatraStores
        .keyValueStoreBuilder(
          NullStatsReceiver,
          FinatraStores.persistentKeyValueStore(stateStoreName),
          Serdes.String,
          Serdes.String
        )
        .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
    )

    builder.addStateStore(
      FinatraTransformer
        .timerStore(name = timerStoreName, timerKeySerde = Serdes.String, statsReceiver = NullStatsReceiver)
    )
  }

  test("FinatraTransformer with changelog enabled") {
    val testKey = "testKey"
    val testValue = "testValue"

    sourceTopic.pipeInput(
      key = testKey,
      value = testValue,
      timestamp = startTime.getMillis)

    val sinkTopicOutput = sinkTopic.readAllOutput()
    val stateStoreChangelogTopicOutput = stateStoreChangelogTopic.readAllOutput()
    val timerStoreChangelogTopicOutput = timerStoreChangelogTopic.readAllOutput()

    sinkTopicOutput.length should be(1)
    stateStoreChangelogTopicOutput.length should be(1)
    timerStoreChangelogTopicOutput.length should be(1)

    stateStoreTestProbe().get(testKey) should be(testValue)
    val timerStoreEntities = timerStoreTestProbe().all().toSeqWithAutoClose
    timerStoreEntities.length should be(1)
  }
}

class WithLoggingDisabled extends FinatraTransformerChangeLogConfigFeatureTest {
  override def configureStateStores(builder: StreamsBuilder): Unit = {
    builder.addStateStore(
      FinatraStores
        .keyValueStoreBuilder(
          NullStatsReceiver,
          FinatraStores.persistentKeyValueStore(stateStoreName),
          Serdes.String,
          Serdes.String
        ).withLoggingDisabled()
    )

    builder.addStateStore(
      FinatraTransformer
        .timerStore(name = timerStoreName, timerKeySerde = Serdes.String, statsReceiver = NullStatsReceiver)
        .withLoggingDisabled()
    )
  }

  test("FinatraTransformer with changelog disabled") {
    val testKey = "testKey"
    val testValue = "testValue"

    sourceTopic.pipeInput(
      key = testKey,
      value = testValue,
      timestamp = startTime.getMillis)

    val sinkTopicOutput = sinkTopic.readAllOutput()
    val stateStoreChangelogTopicOutput = stateStoreChangelogTopic.readAllOutput()
    val timerStoreChangelogTopicOutput = timerStoreChangelogTopic.readAllOutput()

    sinkTopicOutput.length should be(1)
    stateStoreChangelogTopicOutput.length should be(0)
    timerStoreChangelogTopicOutput.length should be(0)

    stateStoreTestProbe().get(testKey) should be(testValue)
    val timerStoreEntities = timerStoreTestProbe().all().toSeqWithAutoClose
    timerStoreEntities.length should be(1)
  }
}
