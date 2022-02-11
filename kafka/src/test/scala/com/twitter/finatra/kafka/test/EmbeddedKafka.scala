package com.twitter.finatra.kafka.test

import com.twitter.conversions.DurationOps._
import com.twitter.conversions.StorageUnitOps._
import com.twitter.finatra.kafka.modules.KafkaBootstrapModule
import com.twitter.inject.Test
import com.twitter.util.Duration
import com.twitter.util.logging.Logging
import java.util.Properties
import kafka.server.KafkaConfig
import org.apache.kafka.common.serialization._
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.integration.utils.EmbeddedKafkaCluster
import org.apache.kafka.streams.integration.utils.KafkaEmbedded
import scala.collection.mutable.ArrayBuffer

trait EmbeddedKafka extends Test with Logging {

  private val kafkaTopics = ArrayBuffer[KafkaTopic[_, _]]()

  /* Protected */

  protected def numKafkaBrokers: Int = 1

  protected def autoCreateTopicsEnable: Boolean = false

  protected def groupInitialRebalanceDelay: Duration = 0.seconds

  protected def maxMessageBytes: Long = 20.megabytes.bytes

  protected val emptyBytes: Bytes = Bytes.wrap(Array.emptyByteArray)

  protected def brokerConfig: Properties = {
    val properties = new Properties()
    properties.put(KafkaConfig.AutoCreateTopicsEnableProp, autoCreateTopicsEnable.toString)
    properties.put(
      KafkaConfig.GroupInitialRebalanceDelayMsProp,
      groupInitialRebalanceDelay.inMillis.toString
    )
    properties.put(KafkaConfig.NumIoThreadsProp, "1")
    properties.put(KafkaConfig.NumNetworkThreadsProp, "1")
    properties.put(KafkaConfig.BackgroundThreadsProp, "1")
    properties.put(KafkaConfig.LogCleanerThreadsProp, "1")
    properties.put(KafkaConfig.DefaultReplicationFactorProp, "1")
    properties.put(KafkaConfig.TransactionsTopicReplicationFactorProp, "1")
    properties.put(KafkaConfig.TransactionsTopicMinISRProp, "1")
    properties.put(KafkaConfig.OffsetsTopicReplicationFactorProp, "1")
    properties.put(KafkaConfig.MinInSyncReplicasProp, "1")
    properties.put(KafkaConfig.MessageMaxBytesProp, maxMessageBytes.toString)
    properties
  }

  protected lazy val kafkaCluster = new EmbeddedKafkaCluster(numKafkaBrokers, brokerConfig)

  protected def brokers: Array[KafkaEmbedded] = {
    val brokersField = classOf[EmbeddedKafkaCluster].getDeclaredField("brokers")
    brokersField.setAccessible(true)
    brokersField.get(kafkaCluster).asInstanceOf[Array[KafkaEmbedded]]
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    kafkaCluster.start()
    createKafkaTopics(kafkaTopics.toSeq)
  }

  protected def kafkaBootstrapFlag: Map[String, String] = {
    Map(KafkaBootstrapModule.kafkaBootstrapServers.name -> kafkaCluster.bootstrapServers())
  }

  protected def kafkaTopic[K, V](
    keySerde: Serde[K],
    valSerde: Serde[V],
    name: String,
    partitions: Int = 1,
    replication: Int = 1,
    autoCreate: Boolean = true,
    autoConsume: Boolean = true,
    logPublishes: Boolean = false,
    allowPublishes: Boolean = true
  ): KafkaTopic[K, V] = {
    val topic = KafkaTopic(
      topic = name,
      keySerde = keySerde,
      valSerde = valSerde,
      _kafkaCluster = () => kafkaCluster,
      partitions = partitions,
      replication = replication,
      autoCreate = autoCreate,
      autoConsume = autoConsume,
      logPublishes = logPublishes,
      allowPublishes = allowPublishes
    )

    kafkaTopics += topic
    topic
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    try {
      debug("Shutdown kafka topics")
      kafkaTopics.foreach(_.close())
    } finally {
      debug("Shutdown embedded kafka")
      closeEmbeddedKafka()
      debug("Embedded kafka closed")
    }
  }

  //Note: EmbeddedKafkaCluster appears to only be closable through a JUnit ExternalResource
  protected def closeEmbeddedKafka(): Unit = {
    val afterMethod = classOf[EmbeddedKafkaCluster].getDeclaredMethod("after")
    afterMethod.setAccessible(true)
    afterMethod.invoke(kafkaCluster)
  }

  protected def createKafkaServerProperties(): Properties = {
    val kafkaServerProperties = new Properties
    kafkaServerProperties.put(KafkaConfig.OffsetsTopicReplicationFactorProp, "1")
    kafkaServerProperties
  }

  private def createKafkaTopics(topics: Seq[KafkaTopic[_, _]]): Unit = {
    for (topic <- topics) {
      if (topic.autoCreate) {
        info("Creating topic " + topic.toPrettyString)
        kafkaCluster.createTopic(topic.topic, topic.partitions, topic.replication)
      }
      topic.init()
    }
  }
}
