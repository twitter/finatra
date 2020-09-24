package com.twitter.finatra.kafkastreams.test

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.test._
import com.twitter.finatra.kafkastreams.internal.utils.CompatibleUtils
import com.twitter.inject.Test
import com.twitter.util.Duration
import java.io.File
import org.apache.kafka.common.serialization.Serde

/**
 * Extensible abstract test class used when testing a single KafkaStreamsTwitterServer in your test.
 */
abstract class KafkaStreamsFeatureTest extends AbstractKafkaStreamsFeatureTest with KafkaFeatureTest

/**
 * Extensible abstract test class used when testing multiple KafkaStreamsTwitterServers in your
 * test.
 */
abstract class KafkaStreamsMultiServerFeatureTest extends AbstractKafkaStreamsFeatureTest

/**
 * Extensible abstract test class that provides helper methods to create and access the kafka
 * topics used in testing.
 */
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
      "kafka.consumer.auto.offset.reset" -> "earliest",
      "kafka.consumer.max.poll.records" -> "1", //Read one record at a time to help makes tests more deterministic
      "kafka.commit.interval" -> kafkaCommitInterval.toString(),
      "kafka.replication.factor" -> "1",
      "kafka.state.dir" -> newTempDirectory().toString
    )
  }

  /**
   * Creates a kafka topic on the internal test brokers to be used for testing.  Returns an instance
   * of a [[KafkaTopic]] which can be used in testing to write to/read from the topic.
   *
   * @param keySerde serde for the key of the topic
   * @param valSerde serde for the value of the topic
   * @param name name of the topic
   * @param partitions number of partitions of the topic
   * @param replication replication factor for the topic
   * @param autoCreate true to create the topic on the brokers, false to simply access an existing
   *                   topic.
   * @param autoConsume true causes the [[KafkaTopic]] class to read messages off of the topic as
   *                    soon as they are available, false leaves them on the topic until
   *                    [[KafkaTopic.consumeRecord()]] is called.
   * @param logPublish true will log each
   * @param allowPublishes whether or not this topic allows you to publish to it from a test
   * @tparam K the type of the key
   * @tparam V the type of the value
   *
   * @return a kafkaTopic which can be used to read and assert that values were written to a topic,
   *         or insert values into a topic.
   */
  override protected def kafkaTopic[K, V](
    keySerde: Serde[K],
    valSerde: Serde[V],
    name: String,
    partitions: Int = 1,
    replication: Int = 1,
    autoCreate: Boolean = true,
    autoConsume: Boolean = true,
    logPublish: Boolean = false,
    allowPublishes: Boolean = true // TODO is this used?!?!?
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

  /**
   * Returns an instance of a [[KafkaTopic]] which can be used in testing to write to/read from the
   * topic.
   *
   * @note because changelog topics are automatically created by the KafkaStreams app, use this
   *       method to access them, which will not create them on the broker.
   *
   * @note you cannot publish to this [[KafkaTopic]] from your test, because all publishes to this
   *       topic should originate from KafkaStreams.  Attempting to do so will cause an assertion
   *       to fail.
   *
   * @param keySerde serde for the key of the topic
   * @param valSerde serde for the value of the topic
   * @param name name of the topic
   * @param partitions number of partitions of the topic
   * @param replication replication factor for the topic
   * @param autoCreate true to create the topic on the brokers, false to simply access an existing
   *                   topic.
   * @tparam K the type of the key
   * @tparam V the type of the value
   *
   * @return a kafkaTopic which can be used to assert that values have been written to the changelog.
   */
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

  protected def resetStreamThreadId(): Unit = {
    CompatibleUtils.resetStreamThreadId()
  }
}
