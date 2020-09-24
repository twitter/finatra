package com.twitter.finatra.kafkastreams.config

import com.twitter.finatra.kafka.config.{KafkaConfigMethods, ToKafkaProperties}
import com.twitter.finatra.kafka.consumers.KafkaConsumerConfigMethods
import com.twitter.finatra.kafka.producers.KafkaProducerConfigMethods
import com.twitter.finatra.kafka.utils.BootstrapServerUtils
import com.twitter.finatra.kafkastreams.domain.ProcessingGuarantee
import com.twitter.util.{Duration, StorageUnit}
import java.util.Properties
import org.apache.kafka.common.metrics.Sensor.RecordingLevel
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.streams.StreamsConfig

/**
 * Builder for setting various [[StreamsConfig]] parameters, see that class for documentation on
 * each parameter.
 */
class KafkaStreamsConfig(
  streamsConfigMap: Map[String, String] = Map.empty,
  producerConfigMap: Map[String, String] = Map.empty,
  consumerConfigMap: Map[String, String] = Map.empty)
    extends KafkaConfigMethods[KafkaStreamsConfig]
    with ToKafkaProperties {
  override protected def fromConfigMap(configMap: Map[String, String]): KafkaStreamsConfig =
    new KafkaStreamsConfig(configMap, producerConfigMap, consumerConfigMap)

  override protected def configMap: Map[String, String] = streamsConfigMap

  val producer: KafkaProducerConfigMethods[KafkaStreamsConfig] =
    new KafkaProducerConfigMethods[KafkaStreamsConfig] {
      override protected def fromConfigMap(configMap: Map[String, String]): This =
        new KafkaStreamsConfig(streamsConfigMap, configMap, consumerConfigMap)
      override protected def configMap: Map[String, String] = producerConfigMap
    }

  val consumer: KafkaConsumerConfigMethods[KafkaStreamsConfig] =
    new KafkaConsumerConfigMethods[KafkaStreamsConfig] {
      override protected def fromConfigMap(configMap: Map[String, String]): This =
        new KafkaStreamsConfig(streamsConfigMap, producerConfigMap, configMap)

      override protected def configMap: Map[String, String] = consumerConfigMap
    }

  override def properties: Properties = {
    val streamsProperties = super.properties

    for ((k, v) <- producerConfigMap) {
      streamsProperties.put(StreamsConfig.producerPrefix(k), v)
    }

    for ((k, v) <- consumerConfigMap) {
      streamsProperties.put(StreamsConfig.consumerPrefix(k), v)
    }

    streamsProperties
  }

  def dest(dest: String): This = bootstrapServers(BootstrapServerUtils.lookupBootstrapServers(dest))

  def applicationId(appId: String): This =
    withConfig(StreamsConfig.APPLICATION_ID_CONFIG, appId)

  def applicationServer(hostPort: String): This =
    withConfig(StreamsConfig.APPLICATION_SERVER_CONFIG, hostPort)

  def bootstrapServers(servers: String): This =
    withConfig(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, servers)

  def bufferedRecordsPerPartition(records: Int): This =
    withConfig(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, records.toString)

  def cacheMaxBuffering(storageUnit: StorageUnit): This =
    withConfig(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, storageUnit)

  def clientId(clientId: String): This =
    withConfig(StreamsConfig.CLIENT_ID_CONFIG, clientId)

  def commitInterval(duration: Duration): This =
    withConfig(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, duration)

  def connectionsMaxIdle(duration: Duration): This =
    withConfig(StreamsConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, duration)

  def defaultDeserializationExceptionHandler[T: Manifest]: This =
    withClassName[T](StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG)

  def defaultKeySerde[T: Manifest]: This =
    withClassName[T](StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG)

  def defaultTimestampExtractor[T: Manifest]: This =
    withClassName[T](StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG)

  def defaultValueSerde[T: Manifest]: This =
    withClassName[T](StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG)

  def numStandbyReplicas(threads: Int): This =
    withConfig(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, threads.toString)

  def numStreamThreads(threads: Int): This =
    withConfig(StreamsConfig.NUM_STREAM_THREADS_CONFIG, threads.toString)

  def metadataMaxAge(duration: Duration): This =
    withConfig(StreamsConfig.METADATA_MAX_AGE_CONFIG, duration)

  def metricReporter[T: Manifest]: This =
    withClassName[T](StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG)

  def metricsNumSamples(samples: Int): This =
    withConfig(StreamsConfig.METRICS_NUM_SAMPLES_CONFIG, samples.toString)

  def metricsRecordingLevelConfig(recordingLevel: RecordingLevel): This =
    withConfig(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, recordingLevel.name)

  def metricsSampleWindow(duration: Duration): This =
    withConfig(StreamsConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, duration)

  def partitionGrouper[T: Manifest]: This =
    withClassName[T](StreamsConfig.PARTITION_GROUPER_CLASS_CONFIG)

  def poll(duration: Duration): This =
    withConfig(StreamsConfig.POLL_MS_CONFIG, duration)

  def processingGuarantee(guarantee: ProcessingGuarantee): This =
    withConfig(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, guarantee.toString)

  def receiveBuffer(storageUnit: StorageUnit): This =
    withConfig(StreamsConfig.RECEIVE_BUFFER_CONFIG, storageUnit)

  def reconnectBackoffMax(duration: Duration): This =
    withConfig(StreamsConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, duration)

  def reconnectBackoff(duration: Duration): This =
    withConfig(StreamsConfig.RECONNECT_BACKOFF_MS_CONFIG, duration)

  def replicationFactor(factor: Int): This =
    withConfig(StreamsConfig.REPLICATION_FACTOR_CONFIG, factor.toString)

  def requestTimeout(duration: Duration): This =
    withConfig(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, duration)

  def retries(retries: Int): This =
    withConfig(StreamsConfig.RETRIES_CONFIG, retries.toString)

  def retryBackoff(duration: Duration): This =
    withConfig(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, duration)

  def rocksDbConfigSetter[T: Manifest]: This =
    withClassName[T](StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG)

  def securityProtocol(securityProtocol: SecurityProtocol): This =
    withConfig(StreamsConfig.SECURITY_PROTOCOL_CONFIG, securityProtocol.name)

  def sendBuffer(storageUnit: StorageUnit): This =
    withConfig(StreamsConfig.SEND_BUFFER_CONFIG, storageUnit)

  def stateCleanupDelay(duration: Duration): This =
    withConfig(StreamsConfig.STATE_CLEANUP_DELAY_MS_CONFIG, duration)

  def stateDir(directory: String): This =
    withConfig(StreamsConfig.STATE_DIR_CONFIG, directory)

  def upgradeFrom(version: String): This =
    withConfig(StreamsConfig.UPGRADE_FROM_CONFIG, version)

  def windowStoreChangeLogAdditionalRetention(duration: Duration): This =
    withConfig(StreamsConfig.WINDOW_STORE_CHANGE_LOG_ADDITIONAL_RETENTION_MS_CONFIG, duration)
}
