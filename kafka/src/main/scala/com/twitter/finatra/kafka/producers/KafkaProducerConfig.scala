package com.twitter.finatra.kafka.producers

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.config.KafkaConfigMethods
import com.twitter.finatra.kafka.config.ToKafkaProperties
import com.twitter.finatra.kafka.domain.AckMode
import com.twitter.finatra.kafka.interceptors.PublishTimeProducerInterceptor
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafka.utils.BootstrapServerUtils
import com.twitter.util.Duration
import com.twitter.util.StorageUnit
import com.twitter.util.logging.Logging
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.metrics.Sensor.RecordingLevel
import org.apache.kafka.common.record.CompressionType

object KafkaProducerConfig {
  val FinagleDestKey: String = "finagle.dest"

  def apply(): KafkaProducerConfig =
    new KafkaProducerConfig()
      .ackMode(AckMode.ALL) // kafka default is AckMode.ONE
      .linger(5.milliseconds)
      .metricReporter[KafkaFinagleMetricsReporter]
      .metricsRecordingLevel(RecordingLevel.INFO)
      .metricsSampleWindow(60.seconds)
      .interceptor[PublishTimeProducerInterceptor]
}

trait KafkaProducerConfigMethods[Self] extends KafkaConfigMethods[Self] with Logging {
  import KafkaProducerConfig.FinagleDestKey

  /**
   * Configure the Kafka server the consumer will connect to. This will resolve the dest to the Kafka server name.
   * The call will block indefinitely until it successfully succeed or failed to resolve the server
   *
   * @param dest the Kafka server address
   * @return the [[KafkaProducerConfigMethods]] instance.
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
   * This will block for up to 'timeout' when attempting to resolve the dest to a kafka server name
   *
   * @param dest the Kafka server address
   * @param timeout the timeout duration when trying to resolve the [[dest]] server.
   * @return the [[KafkaProducerConfigMethods]] instance.
   */
  def dest(dest: String, timeout: Duration): This = {
    val servers = BootstrapServerUtils.lookupBootstrapServers(dest, timeout)
    withConfig(
      Map(
        FinagleDestKey -> dest,
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> servers
      ))
  }

  def ackMode(ackMode: AckMode): This =
    withConfig(ProducerConfig.ACKS_CONFIG, ackMode.toString)

  def batchSize(size: StorageUnit): This =
    withConfig(ProducerConfig.BATCH_SIZE_CONFIG, size)

  def bootstrapServers(servers: String): This =
    withConfig(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers)

  def bufferMemorySize(size: StorageUnit): This =
    withConfig(ProducerConfig.BUFFER_MEMORY_CONFIG, size)

  def clientId(clientId: String): This =
    withConfig(ProducerConfig.CLIENT_ID_CONFIG, clientId)

  def compressionType(compressionType: CompressionType): This =
    withConfig(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType.name)

  def connectionsMaxIdle(duration: Duration): This =
    withConfig(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, duration)

  def deliveryTimeout(duration: Duration): This =
    withConfig(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, duration)

  def enableIdempotence(boolean: Boolean): This =
    withConfig(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, boolean.toString)

  def interceptor[T: Manifest]: This = {
    val interceptorKey = ProducerConfig.INTERCEPTOR_CLASSES_CONFIG

    configMap.get(interceptorKey) match {
      case Some(interceptors)
          if interceptors.split(",").contains(manifest[T].runtimeClass.getName) =>
        warn(
          s"Appending duplicate producer interceptor class name ${manifest[T].runtimeClass.getName} in $interceptors ignored"
        )
        fromConfigMap(configMap)
      case _ =>
        withClassNameBuilder(interceptorKey)
    }
  }

  def linger(duration: Duration): This =
    withConfig(ProducerConfig.LINGER_MS_CONFIG, duration)

  def maxBlock(duration: Duration): This =
    withConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, duration)

  def maxInFlightRequestsPerConnection(max: Int): This =
    withConfig(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, max.toString)

  def maxRequestSize(size: StorageUnit): This =
    withConfig(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, size)

  def metadataMaxAge(duration: Duration): This =
    withConfig(ProducerConfig.METADATA_MAX_AGE_CONFIG, duration)

  def metricReporter[T: Manifest]: This =
    withClassName[T](ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG)

  def metricsSampleWindow(duration: Duration): This =
    withConfig(ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, duration)

  def metricsNumSamples(samples: Int): This =
    withConfig(ProducerConfig.METRICS_NUM_SAMPLES_CONFIG, samples.toString)

  def metricsRecordingLevel(recordingLevel: RecordingLevel): This =
    withConfig(ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG, recordingLevel.name)

  def partitioner[T: Manifest]: This =
    withClassName[T](ProducerConfig.PARTITIONER_CLASS_CONFIG)

  def receiveBufferSize(size: StorageUnit): This =
    withConfig(ProducerConfig.RECEIVE_BUFFER_CONFIG, size)

  def reconnectBackoffMax(duration: Duration): This =
    withConfig(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, duration)

  def reconnectBackoff(duration: Duration): This =
    withConfig(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, duration)

  def requestTimeout(duration: Duration): This =
    withConfig(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, duration)

  def retries(retries: Int): This =
    withConfig(ProducerConfig.RETRIES_CONFIG, retries.toString)

  def retryBackoff(duration: Duration): This =
    withConfig(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, duration)

  def sendBufferSize(size: StorageUnit): This =
    withConfig(ProducerConfig.SEND_BUFFER_CONFIG, size)

  def transactionalId(id: String): This =
    withConfig(ProducerConfig.TRANSACTIONAL_ID_CONFIG, id)

  def transactionTimeout(duration: Duration): This =
    withConfig(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, duration)

  // Unsupported. Pass instances directly to the producer instead.
  // ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
  // ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
}

case class KafkaProducerConfig private (configMap: Map[String, String] = Map.empty)
    extends KafkaProducerConfigMethods[KafkaProducerConfig]
    with ToKafkaProperties {
  override def fromConfigMap(config: Map[String, String]): KafkaProducerConfig =
    KafkaProducerConfig(config)
}
