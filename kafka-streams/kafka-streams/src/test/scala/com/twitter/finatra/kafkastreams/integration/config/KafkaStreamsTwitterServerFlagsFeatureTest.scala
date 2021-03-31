package com.twitter.finatra.kafkastreams.integration.config

import com.twitter.finatra.kafka.serde.{UnKeyed, UnKeyedSerde}
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}
import org.joda.time.DateTime

abstract class KafkaStreamsTwitterServerFlagsFeatureTest(flags: Map[String, String])
  extends TopologyFeatureTest {
  private val appId = "no-op"

  private val kafkaStreamsTwitterServer: KafkaStreamsTwitterServer = new KafkaStreamsTwitterServer {
    override val name: String = appId

    override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
      val finatraTransformerSupplier = () =>
        new FinatraTransformer[UnKeyed, String, UnKeyed, String](statsReceiver = statsReceiver) {
          override def onInit(): Unit = {}
          override def onClose(): Unit = {}
          override def onMessage(messageTime: Time, unKeyed: UnKeyed, string: String): Unit = {}
        }

      builder.asScala
        .stream("source")(Consumed.`with`(UnKeyedSerde, Serdes.String))
        .transform(finatraTransformerSupplier)
        .to("sink")(Produced.`with`(UnKeyedSerde, Serdes.String))
    }

    override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
      super.streamsProperties(config)
    }
  }

  private val _topologyTester = FinatraTopologyTester(
    kafkaApplicationId = appId,
    server = kafkaStreamsTwitterServer,
    startingWallClockTime = DateTime.now,
    flags = flags
  )

  override protected def topologyTester: FinatraTopologyTester = {
    _topologyTester
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    topologyTester.reset()
    topologyTester.topic(
      "source",
      UnKeyedSerde,
      Serdes.String
    )
    topologyTester.topic(
      "sink",
      UnKeyedSerde,
      Serdes.String
    )
  }
}

class KafkaStreamsTwitterServerDefaultFlagsFeatureTest
  extends KafkaStreamsTwitterServerFlagsFeatureTest(flags = Map.empty[String, String]) {
  test("KafkaStreamsTwitterServer default flags") {
    val properties = topologyTester.properties
    properties.getProperty("application.id") should be("no-op")
    properties.getProperty("application.server") should be("0.localhost:9990")
    properties.getProperty("bootstrap.servers") should be("127.0.0.1:12345")
    properties.getProperty("cache.max.bytes.buffering") should be("10485760")
    properties.getProperty("commit.interval.ms") should be("30000")
    properties.getProperty("consumer.auto.offset.reset") should be("latest")
    properties.getProperty("consumer.connections.max.idle.ms") should be("540000")
    properties.getProperty("consumer.fetch.max.wait.ms") should be("500")
    properties.getProperty("consumer.fetch.min.bytes") should be("1")
    properties.getProperty("consumer.heartbeat.interval.ms") should be("3000")
    properties.getProperty("consumer.interceptor.classes") should be(
      "com.twitter.finatra.kafkastreams.internal.interceptors" +
        ".KafkaStreamsMonitoringConsumerInterceptor")
    properties.getProperty("consumer.max.partition.fetch.bytes") should be("1048576")
    properties.getProperty("consumer.max.poll.interval.ms") should be("300000")
    properties.getProperty("consumer.max.poll.records") should be("500")
    properties.getProperty("consumer.metric.reporters") should be(
      "com.twitter.finatra.kafkastreams.internal.stats.KafkaStreamsFinagleMetricsReporter")
    properties.getProperty("consumer.metrics.recording.level") should be("INFO")
    properties.getProperty("consumer.metrics.sample.window.ms") should be("60000")
    properties.getProperty("consumer.request.timeout.ms") should be("30000")
    properties.getProperty("consumer.session.timeout.ms") should be("10000")
    properties.getProperty("consumer.stats_scope") should be("kafka")
    properties.getProperty("default.key.serde") should be(
      "com.twitter.finatra.kafkastreams.internal.serde.AvoidDefaultSerde")
    properties.getProperty("default.value.serde") should be(
      "com.twitter.finatra.kafkastreams.internal.serde.AvoidDefaultSerde")
    properties.getProperty("kafka.instance.key").isEmpty should be(true)
    properties.getProperty("metadata.max.age.ms") should be("300000")
    properties.getProperty("metric.reporters") should be(
      "com.twitter.finatra.kafkastreams.internal.stats.KafkaStreamsFinagleMetricsReporter")
    properties.getProperty("metrics.recording.level") should be("INFO")
    properties.getProperty("metrics.sample.window.ms") should be("60000")
    properties.getProperty("num.standby.replicas") should be("0")
    properties.getProperty("num.stream.threads") should be("1")
    properties.getProperty("processing.guarantee") should be("at_least_once")
    properties.getProperty("producer.acks") should be("all")
    properties.getProperty("producer.interceptor.classes") should be(
      "com.twitter.finatra.kafka.interceptors.PublishTimeProducerInterceptor" +
        ",com.twitter.finatra.kafka.interceptors.InstanceMetadataProducerInterceptor")
    properties.getProperty("producer.linger.ms") should be("0")
    properties.getProperty("producer.metric.reporters") should be(
      "com.twitter.finatra.kafkastreams.internal.stats.KafkaStreamsFinagleMetricsReporter")
    properties.getProperty("producer.metrics.recording.level") should be("INFO")
    properties.getProperty("producer.metrics.sample.window.ms") should be("60000")
    properties.getProperty("producer.stats_scope") should be("kafka")
    properties.getProperty("replication.factor") should be("3")
    properties.getProperty("state.dir") should not be empty
    properties.getProperty("stats_scope") should be("kafka")
  }
}

class KafkaStreamsTwitterServerNonDefaultFlagsFeatureTest
  extends KafkaStreamsTwitterServerFlagsFeatureTest(flags = Map(
    "kafka.num.stream.threads" -> "2",
    "kafka.num.standby.replicas" -> "3",
    "kafka.processing.guarantee" -> "exactly_once",
    "kafka.cache.max.bytes.buffering" -> "1231231",
    "kafka.metadata.max.age.ms" -> "323232",
    "kafka.consumer.max.poll.records" -> "200",
    "kafka.consumer.max.poll.interval.ms" -> "232323",
    "kafka.consumer.auto.offset.reset" -> "latest",
    "kafka.consumer.session.timeout.ms" -> "1212",
    "kafka.consumer.heartbeat.interval.ms" -> "5656",
    "kafka.consumer.fetch.min.bytes" -> "22",
    "kafka.consumer.fetch.max.wait.ms" -> "556",
    "kafka.consumer.max.partition.fetch.bytes" -> "12309812",
    "kafka.consumer.request.timeout.ms" -> "33333",
    "kafka.consumer.connections.max.idle.ms" -> "545454",
    "kafka.producer.acks" -> "one",
    "kafka.producer.linger.ms" -> "42",
    "kafka.replication.factor" -> "5",
    "kafka.application.server" -> "localhost:1212",
    "kafka.metrics.recording.level" -> "DEBUG",
    "kafka.commit.interval" -> "60.seconds",
    "kafka.instance.key" -> "non_default_instance_key"
  )) {

  test("KafkaStreamsTwitterServer custom flags") {
    val properties = topologyTester.properties
    properties.getProperty("num.stream.threads") should be("2")
    properties.getProperty("num.standby.replicas") should be("3")
    properties.getProperty("processing.guarantee") should be("exactly_once")
    properties.getProperty("cache.max.bytes.buffering") should be("1231231")
    properties.getProperty("metadata.max.age.ms") should be("323232")
    properties.getProperty("consumer.max.poll.records") should be("200")
    properties.getProperty("consumer.max.poll.interval.ms") should be("232323")
    properties.getProperty("consumer.auto.offset.reset") should be("latest")
    properties.getProperty("consumer.session.timeout.ms") should be("1212")
    properties.getProperty("consumer.heartbeat.interval.ms") should be("5656")
    properties.getProperty("consumer.fetch.min.bytes") should be("22")
    properties.getProperty("consumer.fetch.max.wait.ms") should be("556")
    properties.getProperty("consumer.max.partition.fetch.bytes") should be("12309812")
    properties.getProperty("consumer.request.timeout.ms") should be("33333")
    properties.getProperty("consumer.connections.max.idle.ms") should be("545454")
    properties.getProperty("producer.acks") should be("1")
    properties.getProperty("producer.linger.ms") should be("42")
    properties.getProperty("replication.factor") should be("5")
    properties.getProperty("application.server") should be("localhost:1212")
    properties.getProperty("metrics.recording.level") should be("DEBUG")
    properties.getProperty("commit.interval.ms") should be("60000")
    properties.getProperty("kafka.instance.key") should be("non_default_instance_key")
  }
}
