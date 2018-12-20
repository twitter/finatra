package com.twitter.finatra.kafkastreams.test

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.test._
import com.twitter.inject.Test
import com.twitter.util.Duration
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.processor.internals.StreamThread

abstract class KafkaStreamsFeatureTest extends AbstractKafkaStreamsFeatureTest with KafkaFeatureTest

abstract class KafkaStreamsMultiServerFeatureTest extends AbstractKafkaStreamsFeatureTest

abstract class AbstractKafkaStreamsFeatureTest extends Test with EmbeddedKafka {

  override def afterAll(): Unit = {
    super.afterAll()

    resetStreamThreadId()
  }

  protected def newTempDirectory(): File = {
    TestDirectoryUtils.newTempDirectory()
  }

  protected def kafkaCommitInterval: Duration = 50.milliseconds

  protected def kafkaStreamsFlags: Map[String, String] = {
    kafkaBootstrapFlag ++ Map(
      "kafka.auto.offset.reset" -> "earliest",
      "kafka.max.poll.records" -> "1", //Read one record at a time to help makes tests more deterministic
      "kafka.commit.interval" -> kafkaCommitInterval.toString(),
      "kafka.replication.factor" -> "1",
      "kafka.state.dir" -> newTempDirectory().toString
    )
  }

  override protected def kafkaTopic[K, V](
    keySerde: Serde[K],
    valSerde: Serde[V],
    name: String,
    partitions: Int = 1,
    replication: Int = 1,
    autoCreate: Boolean = true,
    autoConsume: Boolean = true,
    logPublish: Boolean = false,
    allowPublishes: Boolean = true
  ): KafkaTopic[K, V] = {
    if (name.contains("changelog") && autoCreate) {
      warn(
        s"Changelog topics should be created by Kafka-Streams. It's recommended that you set autoCreate=false for kafka topic $name"
      )
    }
    super.kafkaTopic(
      keySerde,
      valSerde,
      name,
      partitions,
      replication,
      autoCreate,
      autoConsume = autoConsume,
      logPublishes = logPublish
    )
  }

  protected def kafkaChangelogTopic[K, V](
    keySerde: Serde[K],
    valSerde: Serde[V],
    name: String,
    partitions: Int = 1,
    replication: Int = 1,
    autoConsume: Boolean = true
  ): KafkaTopic[K, V] = {
    super.kafkaTopic(
      keySerde,
      valSerde,
      name,
      partitions,
      replication,
      autoCreate = false,
      autoConsume = autoConsume,
      logPublishes = false,
      allowPublishes = false
    )
  }

  //HACK: Reset StreamThread's id after each test so that each test starts from a known fresh state
  //Without this hack, tests would need to always wildcard the thread number when asserting stats
  protected def resetStreamThreadId(): Unit = {
    val streamThreadClass = classOf[StreamThread]
    val streamThreadIdSequenceField = streamThreadClass
      .getDeclaredField("STREAM_THREAD_ID_SEQUENCE")
    streamThreadIdSequenceField.setAccessible(true)
    val streamThreadIdSequence = streamThreadIdSequenceField.get(null).asInstanceOf[AtomicInteger]
    streamThreadIdSequence.set(1)
  }
}
