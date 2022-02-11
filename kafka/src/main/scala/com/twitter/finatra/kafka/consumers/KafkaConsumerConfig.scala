package com.twitter.finatra.kafka.consumers

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.config.KafkaConfigMethods
import com.twitter.finatra.kafka.config.ToKafkaProperties
import com.twitter.finatra.kafka.domain.IsolationLevel
import com.twitter.finatra.kafka.domain.KafkaGroupId
import com.twitter.finatra.kafka.interceptors.MonitoringConsumerInterceptor
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafka.utils.BootstrapServerUtils
import com.twitter.util.Duration
import com.twitter.util.StorageUnit
import com.twitter.util.logging.Logging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.metrics.Sensor.RecordingLevel

object KafkaConsumerConfig {
  val FinagleDestKey: String = "finagle.dest"

  def apply(): KafkaConsumerConfig =
    new KafkaConsumerConfig()
      .metricReporter[KafkaFinagleMetricsReporter]
      .metricsRecordingLevel(RecordingLevel.INFO)
      .metricsSampleWindow(60.seconds)
      .interceptor[MonitoringConsumerInterceptor]
}

trait KafkaConsumerConfigMethods[Self] extends KafkaConfigMethods[Self] with Logging {
  import KafkaConsumerConfig.FinagleDestKey

  /**
   * Configure the Kafka server the consumer will connect to.
   *
   * @param dest the Kafka server address
   * @return the [[KafkaConsumerConfigMethods]] instance.
   */
  def dest(dest: String): This = {
    val servers = BootstrapServerUtils.lookupBootstrapServers(dest)
    withConfig(
      Map(
        FinagleDestKey -> dest,
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> servers
      ))
  }

  /**
   * Configure the Kafka server the consumer will connect to.
   *
   * @param dest the Kafka server address
   * @param timeout the timeout duration when trying to resolve the [[dest]] server.
   * @return the [[KafkaConsumerConfigMethods]] instance.
   */
  def dest(dest: String, timeout: Duration): This = {
    val servers = BootstrapServerUtils.lookupBootstrapServers(dest, timeout)
    withConfig(
      Map(
        FinagleDestKey -> dest,
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> servers
      ))
  }

  def autoCommitInterval(duration: Duration): This =
    withConfig(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, duration)

  def autoOffsetReset(offsetResetStrategy: OffsetResetStrategy): This =
    withConfig(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetStrategy.toString.toLowerCase)

  def bootstrapServers(servers: String): This =
    withConfig(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers)

  def checkCrcs(boolean: Boolean): This =
    withConfig(ConsumerConfig.CHECK_CRCS_CONFIG, boolean.toString)

  def clientId(clientId: String): This =
    withConfig(ConsumerConfig.CLIENT_ID_CONFIG, clientId)

  def connectionsMaxIdle(duration: Duration): This =
    withConfig(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, duration)

  def enableAutoCommit(boolean: Boolean): This =
    withConfig(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, boolean.toString)

  def excludeInternalTopics(boolean: Boolean): This =
    withConfig(ConsumerConfig.EXCLUDE_INTERNAL_TOPICS_CONFIG, boolean.toString)

  def fetchMax(storageUnit: StorageUnit): This =
    withConfig(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, storageUnit)

  def fetchMaxWait(duration: Duration): This =
    withConfig(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, duration)

  def fetchMin(storageUnit: StorageUnit): This =
    withConfig(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, storageUnit)

  def groupId(groupId: KafkaGroupId): This =
    withConfig(ConsumerConfig.GROUP_ID_CONFIG, groupId.name)

  def heartbeatInterval(duration: Duration): This =
    withConfig(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, duration)

  def interceptor[T: Manifest]: This = {
    val interceptorKey = ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG
    configMap.get(interceptorKey) match {
      case Some(interceptors)
          if interceptors.split(",").contains(manifest[T].runtimeClass.getName) =>
        warn(
          s"Appending duplicate consumer interceptor class name ${manifest[T].runtimeClass.getName} in $interceptors ignored"
        )
        fromConfigMap(configMap)
      case _ =>
        withClassNameBuilder(interceptorKey)
    }
  }

  def isolationLevel(isolationLevel: IsolationLevel): This =
    withConfig(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel.toString)

  def maxPartitionFetch(storageUnit: StorageUnit): This =
    withConfig(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, storageUnit)

  def maxPollInterval(duration: Duration): This =
    withConfig(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, duration)

  def maxPollRecords(int: Int): This =
    withConfig(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, int.toString)

  def metadataMaxAge(duration: Duration): This =
    withConfig(ConsumerConfig.METADATA_MAX_AGE_CONFIG, duration)

  def metricReporter[T: Manifest]: This =
    withClassName[T](ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG)

  def metricsNumSamples(int: Int): This =
    withConfig(ConsumerConfig.METRICS_NUM_SAMPLES_CONFIG, int.toString)

  def metricsRecordingLevel(recordingLevel: RecordingLevel): This =
    withConfig(ConsumerConfig.METRICS_RECORDING_LEVEL_CONFIG, recordingLevel.name)

  def metricsSampleWindow(duration: Duration): This =
    withConfig(ConsumerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, duration)

  def partitionAssignmentStrategy[T: Manifest]: This =
    withClassName(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG)

  def receiveBuffer(storageUnit: StorageUnit): This =
    withConfig(ConsumerConfig.RECEIVE_BUFFER_CONFIG, storageUnit)

  def reconnectBackoffMax(duration: Duration): This =
    withConfig(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, duration)

  def reconnectBackoff(duration: Duration): This =
    withConfig(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, duration)

  def requestTimeout(duration: Duration): This =
    withConfig(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, duration)

  def retryBackoff(duration: Duration): This =
    withConfig(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, duration)

  def sendBufferConfig(storageUnit: StorageUnit): This =
    withConfig(ConsumerConfig.SEND_BUFFER_CONFIG, storageUnit)

  def sessionTimeout(duration: Duration): This =
    withConfig(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, duration)

  // Use "group.instance.id" now as ConsumerConfig.GROUP_INSTANCE_ID_CONFIG is ony defined
  // from 2.4+
  def groupInstanceId(instanceId: String): This =
    withConfig("group.instance.id", instanceId)

  // Unsupported. Pass instances directly to the consumer instead.
  // ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
  // ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
}

case class KafkaConsumerConfig private (configMap: Map[String, String] = Map.empty)
    extends KafkaConsumerConfigMethods[KafkaConsumerConfig]
    with ToKafkaProperties {

  override def fromConfigMap(config: Map[String, String]): KafkaConsumerConfig =
    KafkaConsumerConfig(config)
}
