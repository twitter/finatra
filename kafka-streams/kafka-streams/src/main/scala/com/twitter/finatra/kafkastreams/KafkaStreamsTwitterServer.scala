package com.twitter.finatra.kafkastreams

import com.twitter.app.Flag
import com.twitter.conversions.DurationOps._
import com.twitter.conversions.StorageUnitOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.domain.AckMode
import com.twitter.finatra.kafka.interceptors.{
  InstanceMetadataProducerInterceptor,
  MonitoringConsumerInterceptor,
  PublishTimeProducerInterceptor
}
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafkastreams.config.{FinatraRocksDBConfig, KafkaStreamsConfig}
import com.twitter.finatra.kafkastreams.domain.ProcessingGuarantee
import com.twitter.finatra.kafkastreams.internal.admin.AdminRoutes
import com.twitter.finatra.kafkastreams.internal.interceptors.KafkaStreamsMonitoringConsumerInterceptor
import com.twitter.finatra.kafkastreams.internal.listeners.FinatraStateRestoreListener
import com.twitter.finatra.kafkastreams.internal.serde.AvoidDefaultSerde
import com.twitter.finatra.kafkastreams.internal.stats.KafkaStreamsFinagleMetricsReporter
import com.twitter.finatra.kafkastreams.internal.utils.KafkaFlagUtils
import com.twitter.finatra.kafkastreams.utils.ScalaStreamsImplicits
import com.twitter.inject.server.TwitterServer
import com.twitter.util.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong
import org.apache.kafka.clients.consumer.{ConsumerConfig, OffsetResetStrategy}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.metrics.Sensor.RecordingLevel
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.streams.KafkaStreams.{State, StateListener}
import org.apache.kafka.streams.{
  KafkaClientSupplier,
  KafkaStreams,
  StreamsBuilder,
  StreamsConfig,
  Topology
}

/**
 * A [[com.twitter.server.TwitterServer]] that supports configuring a KafkaStreams topology.
 *
  * To use, override the [[configureKafkaStreams]] method to setup your topology.
 *
  * {{{
 *   import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
 *
  *   object MyKafkaStreamsTwitterServerMain extends MyKafkaStreamsTwitterServer
 *
  *   class MyKafkaStreamsTwitterServer extends KafkaStreamsTwitterServer {
 *
  *   override def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {
 *     streamsBuilder.asScala
 *       .stream("dp-it-devel-tweetid-to-interaction")(
 *         Consumed.`with`(ScalaSerdes.Long, ScalaSerdes.Thrift[MigratorInteraction])
 *       )
 *   }
 * }}}
 */
abstract class KafkaStreamsTwitterServer
    extends TwitterServer
    with KafkaFlagUtils
    with ScalaStreamsImplicits {

  // Required configs
  protected[kafkastreams] val applicationId =
    requiredKafkaFlag[String](StreamsConfig.APPLICATION_ID_CONFIG)
  protected[kafkastreams] val bootstrapServer = requiredKafkaFlag[String](
    StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
    helpPrefix = "A finagle destination or"
  )

  // StreamsConfig default flags
  private val numStreamThreads =
    flagWithKafkaDefault[Integer](StreamsConfig.NUM_STREAM_THREADS_CONFIG)
  private val numStandbyReplicas =
    flagWithKafkaDefault[Integer](StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG)
  private val processingGuarantee =
    flagWithKafkaDefault[String](StreamsConfig.PROCESSING_GUARANTEE_CONFIG)
  private val cacheMaxBytesBuffering =
    flagWithKafkaDefault[Long](StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG)
  private val metadataMaxAge =
    flagWithKafkaDefault[Long](StreamsConfig.METADATA_MAX_AGE_CONFIG)

  // ConsumerConfig default flags
  private val consumerMaxPollRecords =
    consumerFlagWithKafkaDefault[Int](ConsumerConfig.MAX_POLL_RECORDS_CONFIG)
  private val consumerMaxPollInterval =
    consumerFlagWithKafkaDefault[Int](
      ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG
    )
  private val consumerAutoOffsetReset =
    consumerFlagWithKafkaDefault[String](
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
    )
  private val consumerSessionTimeout =
    consumerFlagWithKafkaDefault[Int](ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG)
  private val consumerHeartbeatInterval =
    consumerFlagWithKafkaDefault[Int](
      ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG
    )
  private val consumerFetchMin =
    consumerFlagWithKafkaDefault[Int](ConsumerConfig.FETCH_MIN_BYTES_CONFIG)
  private val consumerFetchMaxWait =
    consumerFlagWithKafkaDefault[Int](ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG)
  private val consumerMaxPartitionFetch =
    consumerFlagWithKafkaDefault[Int](
      ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG
    )
  private val consumerRequestTimeout =
    consumerFlagWithKafkaDefault[Int](ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG)
  private val consumerConnectionsMaxIdle =
    consumerFlagWithKafkaDefault[Long](
      ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG
    )

  // ProducerConfig default flag
  private val producerLinger =
    producerFlagWithKafkaDefault[Long](ProducerConfig.LINGER_MS_CONFIG)

  // Configs with customized default
  private val replicationFactor =
    kafkaFlag(
      StreamsConfig.REPLICATION_FACTOR_CONFIG,
      3
    ) // We set it to 3 for durability and reliability.
  protected[kafkastreams] val applicationServerConfig =
    kafkaFlag(
      StreamsConfig.APPLICATION_SERVER_CONFIG,
      s"0.localhost:$defaultAdminPort"
    )
  private[finatra] val stateDir =
    kafkaFlag(StreamsConfig.STATE_DIR_CONFIG, "kafka-stream-state")
  private val metricsRecordingLevel =
    kafkaFlag(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO")

  private val producerAckMode = producerFlag(ProducerConfig.ACKS_CONFIG, "all")

  protected val commitInterval: Flag[Duration] = flag(
    "kafka.commit.interval",
    30.seconds,
    "The frequency with which to save the position of the processor."
  )
  private val instanceKey: Flag[String] = flag(
    InstanceMetadataProducerInterceptor.KafkaInstanceKeyFlagName,
    "",
    "The application specific identifier for process or job that gets added to record header as `instance_key`." +
      "The `instance_key` is only included when this flag is set, otherwise no header will be included."
  )

  @volatile private var timeStartedRebalancingOpt: Option[Long] = None
  private val totalTimeRebalancing: AtomicLong = new AtomicLong(0)

  @volatile private var lastUncaughtException: Throwable = _

  def uncaughtException: Throwable = lastUncaughtException

  protected[kafkastreams] val kafkaStreamsBuilder = new StreamsBuilder()
  protected[kafkastreams] var properties: Properties = _
  protected[kafkastreams] var topology: Topology = _
  protected var kafkaStreams: KafkaStreams = _

  /* Abstract Protected */

  /**
   * Callback method which is executed after the injector is created and before any other lifecycle
   * methods.
   *
    * Use the provided StreamsBuilder to create your KafkaStreams topology.
   *
    * @note It is NOT expected that you block in this method as you will prevent completion
   * of the server lifecycle.
   * @param builder
   */
  protected def configureKafkaStreams(builder: StreamsBuilder): Unit

  /* Protected */

  override val defaultCloseGracePeriod: Duration = 1.minute

  protected def streamsStatsReceiver: StatsReceiver = {
    injector.instance[StatsReceiver].scope("kafka").scope("stream")
  }

  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()
    properties = createKafkaStreamsProperties()
    topology = createKafkaStreamsTopology()
    addAdminRoutes(AdminRoutes(properties, topology))
  }

  override protected def postWarmup(): Unit = {
    super.postWarmup()
    createAndStartKafkaStreams()
  }

  /* Protected */

  protected[finatra] def createAndStartKafkaStreams(): Unit = {
    kafkaStreams = new KafkaStreams(topology, properties, kafkaStreamsClientSupplier)
    setExceptionHandler(kafkaStreams)
    monitorStateChanges(kafkaStreams)
    closeKafkaStreamsOnExit()

    kafkaStreams.start()
    // after upgraded to 2.5, we can move to isRunningOrRebalancing() function
    while (kafkaStreams.state() != KafkaStreams.State.RUNNING &&
      kafkaStreams.state() != KafkaStreams.State.REBALANCING) {
      Thread.sleep(100)
      debug("Waiting for Initial Kafka Streams Startup")
    }
  }

  protected def kafkaStreamsClientSupplier: KafkaClientSupplier = {
    new TracingKafkaClientSupplier
  }

  protected def onStateChange(newState: State, oldState: State): Unit = {}

  protected def setExceptionHandler(streams: KafkaStreams): Unit = {
    streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        error("UncaughtException in thread " + t, e)
        lastUncaughtException = e
      }
    })
  }

  /**
   * Callback method which is executed after the injector is created and before KafkaStreams is
   * configured.
   *
    * Use the provided KafkaStreamsConfig and augment to configure your KafkaStreams topology.
   *
    * Example:
   *
    * {{{
   *   override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
   *     super
   *       .streamsProperties(config)
   *       .retries(60)
   *       .retryBackoff(1.second)
   *       .consumer.sessionTimeout(10.seconds)
   *       .consumer.heartbeatInterval(1.second)
   *       .producer.retries(300)
   *       .producer.retryBackoff(1.second)
   *       .producer.requestTimeout(2.minutes)
   *       .producer.transactionTimeout(2.minutes)
   *       .producer.batchSize(500.kilobytes)
   *   }
   * }}}
   *
    *
   * @param config the default KafkaStreamsConfig defined at [[createKafkaStreamsProperties]]
   *
    * @return a KafkaStreamsConfig with your additional configurations applied.
   */
  protected def streamsProperties(
    config: KafkaStreamsConfig
  ): KafkaStreamsConfig = config

  protected[finatra] def createKafkaStreamsProperties(): Properties = {
    var defaultConfig =
      new KafkaStreamsConfig()
        .metricReporter[KafkaStreamsFinagleMetricsReporter]
        .metricsRecordingLevelConfig(
          RecordingLevel.forName(metricsRecordingLevel())
        )
        .metricsSampleWindow(60.seconds)
        .applicationServer(applicationServerConfig())
        .dest(bootstrapServer())
        .stateDir(stateDir())
        .commitInterval(commitInterval())
        .replicationFactor(replicationFactor())
        .numStreamThreads(numStreamThreads())
        .cacheMaxBuffering(cacheMaxBytesBuffering().bytes)
        .numStandbyReplicas(numStandbyReplicas())
        .metadataMaxAge(metadataMaxAge().milliseconds)
        .processingGuarantee(
          ProcessingGuarantee.valueOf(processingGuarantee().toUpperCase)
        )
        .defaultKeySerde[AvoidDefaultSerde]
        .defaultValueSerde[AvoidDefaultSerde]
        .withConfig(
          InstanceMetadataProducerInterceptor.KafkaInstanceKeyFlagName,
          instanceKey()
        )
        .producer
        .ackMode(AckMode.valueOf(producerAckMode().toUpperCase))
        .producer
        .metricReporter[KafkaStreamsFinagleMetricsReporter]
        .producer
        .metricsRecordingLevel(RecordingLevel.forName(metricsRecordingLevel()))
        .producer
        .metricsSampleWindow(60.seconds)
        .producer
        .interceptor[PublishTimeProducerInterceptor]
        .producer
        .interceptor[InstanceMetadataProducerInterceptor]
        .producer
        .linger(producerLinger().milliseconds)
        .consumer
        .fetchMin(consumerFetchMin().bytes)
        .consumer
        .fetchMaxWait(consumerFetchMaxWait().milliseconds)
        .consumer
        .sessionTimeout(consumerSessionTimeout().milliseconds)
        .consumer
        .heartbeatInterval(consumerHeartbeatInterval().milliseconds)
        .consumer
        .metricReporter[KafkaStreamsFinagleMetricsReporter]
        .consumer
        .metricsRecordingLevel(RecordingLevel.forName(metricsRecordingLevel()))
        .consumer
        .metricsSampleWindow(60.seconds)
        .consumer
        .autoOffsetReset(
          OffsetResetStrategy.valueOf(consumerAutoOffsetReset().toUpperCase)
        )
        .consumer
        .maxPollRecords(consumerMaxPollRecords())
        .consumer
        .maxPollInterval(consumerMaxPollInterval().milliseconds)
        .consumer
        .maxPartitionFetch(consumerMaxPartitionFetch().bytes)
        .consumer
        .requestTimeout(consumerRequestTimeout().milliseconds)
        .consumer
        .connectionsMaxIdle(consumerConnectionsMaxIdle().milliseconds)
        .consumer
        .interceptor[KafkaStreamsMonitoringConsumerInterceptor]

    if (applicationId().nonEmpty) {
      defaultConfig = defaultConfig.applicationId(applicationId())
    }

    // Set to the compatible mode from higher version library, customer can
    // use ProtocolUpgrade mixin to override
    if (!AppInfoParser.getVersion().startsWith("2.2")) {
      // we can't use StreamsConfig.UPGRADE_FROM_22 because the variable is
      // not defined in lower version.
      defaultConfig = defaultConfig.upgradeFrom("2.2")
    }

    val properties = streamsProperties(defaultConfig).properties

    // Extra properties used by KafkaStreamsFinagleMetricsReporter.
    properties.put("stats_scope", "kafka")
    properties.put(StreamsConfig.producerPrefix("stats_scope"), "kafka")
    properties.put(StreamsConfig.consumerPrefix("stats_scope"), "kafka")

    properties
  }

  protected[finatra] def createKafkaStreamsTopology(): Topology = {
    KafkaFinagleMetricsReporter.init(injector)
    MonitoringConsumerInterceptor.init(injector)
    FinatraRocksDBConfig.init(injector)

    configureKafkaStreams(kafkaStreamsBuilder)
    val topology = kafkaStreamsBuilder.build()
    info(topology.describe)
    topology
  }

  /* Private */

  private def closeKafkaStreamsOnExit(): Unit = {
    onExit {
      info("Closing kafka streams")
      try {
        kafkaStreams.close(
          java.time.Duration.ofMillis(defaultCloseGracePeriod.inMillis)
        )
      } catch {
        case e: Throwable =>
          error("Error while closing kafka streams", e)
      }
      info("Closed kafka streams")
    }
  }

  private def monitorStateChanges(streams: KafkaStreams): Unit = {
    streams.setStateListener(new FinatraStateChangeListener(streams))

    streams.setGlobalStateRestoreListener(
      new FinatraStateRestoreListener(streamsStatsReceiver)
    )

    streamsStatsReceiver.provideGauge("totalTimeRebalancing")(
      totalTimeRebalancing.get()
    )

    streamsStatsReceiver.provideGauge("state") {
      streams.state match {
        case State.CREATED => 1
        case State.RUNNING => 2
        case State.REBALANCING => 3
        case State.PENDING_SHUTDOWN => 4
        case State.NOT_RUNNING => 5
        case State.ERROR => 6
      }
    }
  }

  private class FinatraStateChangeListener(streams: KafkaStreams) extends StateListener {
    override def onChange(newState: State, oldState: State): Unit = {
      debug(streams.toString)
      if (newState == State.REBALANCING) {
        timeStartedRebalancingOpt = Some(System.currentTimeMillis())
      } else {
        for (timeStartedRebalancing <- timeStartedRebalancingOpt) {
          totalTimeRebalancing.addAndGet(
            System.currentTimeMillis - timeStartedRebalancing
          )
          timeStartedRebalancingOpt = None
        }
      }

      onStateChange(newState, oldState)

      if (newState == State.ERROR) {
        forkAndCloseServer("State.Error")
      }
    }
  }

  // Note: Kafka feature tests hang without closing the twitter server from a separate thread.
  private def forkAndCloseServer(reason: String): Unit = {
    new Thread {
      override def run(): Unit = {
        info(s"FinatraStreams closing server")
        close(defaultCloseGracePeriod)
      }
    }.start()
  }
}
