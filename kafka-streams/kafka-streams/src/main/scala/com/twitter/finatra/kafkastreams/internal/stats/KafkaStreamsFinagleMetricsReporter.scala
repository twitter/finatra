package com.twitter.finatra.kafkastreams.internal.stats

import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import java.util
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.metrics.KafkaMetric
import org.apache.kafka.common.metrics.Sensor.RecordingLevel

object KafkaStreamsFinagleMetricsReporter {

  /**
   * These metrics are derived from all Sensors configured at the DEBUG RecordingLevel in Kafka Streams version 2.0.0
   */
  val debugMetrics = Set(
    /** Defined in: [[org.apache.kafka.streams.processor.internals.ProcessorNode]] */
    "create-latency-avg",
    "create-latency-max",
    "create-rate",
    "create-total",
    "destroy-latency-avg",
    "destroy-latency-max",
    "destroy-rate",
    "destroy-total",
    "forward-latency-avg",
    "forward-latency-max",
    "forward-rate",
    "forward-total",
    "process-latency-avg",
    "process-latency-max",
    "process-rate",
    "process-total",
    "punctuate-latency-avg",
    "punctuate-latency-max",
    "punctuate-rate",
    "punctuate-total",
    /** Defined in: [[org.apache.kafka.streams.processor.internals.StreamTask]] */
    "commit-latency-avg",
    "commit-latency-max",
    "commit-rate",
    "commit-total",
    /** Defined in:
     * [[org.apache.kafka.streams.state.internals.InnerMeteredKeyValueStore]]
     * [[org.apache.kafka.streams.state.internals.MeteredSessionStore]]
     * [[org.apache.kafka.streams.state.internals.MeteredWindowStore]]
     */
    "all-latency-avg",
    "all-latency-max",
    "all-rate",
    "all-total",
    "delete-latency-avg",
    "delete-latency-max",
    "delete-rate",
    "delete-total",
    "flush-latency-avg",
    "flush-latency-max",
    "flush-rate",
    "flush-total",
    "get-latency-avg",
    "get-latency-max",
    "get-rate",
    "get-total",
    "put-latency-avg",
    "put-latency-max",
    "put-rate",
    "put-total",
    "put-all-latency-avg",
    "put-all-latency-max",
    "put-all-rate",
    "put-all-total",
    "put-if-absent-latency-avg",
    "put-if-absent-latency-max",
    "put-if-absent-rate",
    "put-if-absent-total",
    "range-latency-avg",
    "range-latency-max",
    "range-rate",
    "range-total",
    "restore-latency-avg",
    "restore-latency-max",
    "restore-rate",
    "restore-total",
    /** Defined in: [[org.apache.kafka.streams.state.internals.NamedCache]] */
    "hitRatio-avg",
    "hitRatio-min",
    "hitRatio-max"
  )

  private val rateMetricsToIgnore = Set(
    "commit-rate",
    "poll-rate",
    "process-rate",
    "punctuate-rate",
    "skipped-records-rate",
    "task-closed-rate",
    "task-created-rate"
  )

  /**
   * Disables "noisy", UUID-filled, GlobalTable metrics of the form:
   *   ''kafka/$applicationId_$UUID_{GlobalStreamThread, global_consumer}/...''
   * These metrics are derived from the GlobalStreamThread using the global consumer configuration found in Kafka Streams version 2.0.0
   */
  private val globalTableClientIdPatterns = Set("global-consumer", "GlobalStreamThread")
}

/**
 * Kafka-Streams specific MetricsReporter which adds some additional logic on top of the metrics
 * reporter used for Kafka consumers and producers
 */
private[kafkastreams] class KafkaStreamsFinagleMetricsReporter extends KafkaFinagleMetricsReporter {

  private var includeProcessorNodeId = false
  private var includeGlobalTableMetrics = false
  private var recordingLevel: RecordingLevel = RecordingLevel.INFO

  override def configure(configs: util.Map[String, _]): Unit = {
    super.configure(configs)

    includeProcessorNodeId =
      Option(configs.get("includeProcessorNodeId")).getOrElse("false").toString.toBoolean
    includeGlobalTableMetrics =
      Option(configs.get("includeGlobalTableMetrics")).getOrElse("false").toString.toBoolean
    recordingLevel = RecordingLevel.forName(
      Option(configs.get(CommonClientConfigs.METRICS_RECORDING_LEVEL_CONFIG))
        .getOrElse("INFO").toString)
  }

  override protected def shouldIncludeMetric(metric: KafkaMetric): Boolean = {
    val metricName = metric.metricName()

    if (isDebugMetric(metricName) && (recordingLevel != RecordingLevel.DEBUG)) {
      false
    } else if (KafkaStreamsFinagleMetricsReporter
        .rateMetricsToIgnore(metricName.name())) { // remove any metrics that are already "rated" as these not consistent with other stats: go/jira/DINS-2187
      false
    } else if (isGlobalTableMetric(metricName)) {
      includeGlobalTableMetrics
    } else {
      super.shouldIncludeMetric(metric)
    }
  }

  private val clientIdStreamThreadPattern = """.*StreamThread-(\d+)(.*)""".r

  override protected def parseComponent(clientId: String, group: String): String = {
    val groupComponent = group match {
      case "consumer-metrics" => ""
      case "consumer-coordinator-metrics" => ""
      case "producer-metrics" => ""
      case "kafka-client-metrics" => ""
      case "consumer-fetch-manager-metrics" => "fetch"
      case "producer-topic-metrics" => ""
      case "admin-client-metrics" => ""
      case "stream-metrics" => "stream"
      case "stream-task-metrics" => "stream"
      case "stream-rocksdb-state-metrics" => "stream/rocksdb"
      case "stream-rocksdb-window-metrics" => "stream/rocksdb_window"
      case "stream-in-memory-state-metrics" => "stream/in-memory"
      case "stream-record-cache-metrics" => "stream/record-cache"
      case _ =>
        debug("Dropping Metric Component: " + group)
        ""
    }

    if (clientId == null || clientId.isEmpty) {
      ""
    } else if (clientIdStreamThreadPattern.findFirstIn(clientId).isDefined) {
      val clientIdStreamThreadPattern(threadNumber, clientIdComponent) = clientId
      if (clientIdComponent.isEmpty) {
        "thread" + threadNumber + "/" + groupComponent
      } else {
        "thread" + threadNumber + "/" + clientIdComponent.stripPrefix("-")
      }
    } else {
      clientId
    }
  }

  override protected def createFinagleMetricName(
    metric: KafkaMetric,
    metricName: String,
    allTags: java.util.Map[String, String],
    component: String,
    nodeId: String,
    topic: String
  ): String = {
    val taskId = Option(allTags.remove("task-id")).map("/" + _).getOrElse("")
    val processorNodeId =
      if (!includeProcessorNodeId) ""
      else Option(allTags.remove("processor-node-id")).map("/" + _).getOrElse("")
    val partition = parsePartitionTag(allTags)
    val rocksDbStateId = Option(allTags.remove("rocksdb-state-id")).map("/" + _).getOrElse("")
    val rocksDbWindowId = Option(allTags.remove("rocksdb-window-id")).map("/" + _).getOrElse("")
    val rocksDbWindowStateId =
      Option(allTags.remove("rocksdb-window-state-id")).map("/" + _).getOrElse("")
    val inMemWindowId = Option(allTags.remove("in-mem-window-id")).map("/" + _).getOrElse("")
    val inMemoryStateId = Option(allTags.remove("in-memory-state-id")).map("/" + _).getOrElse("")
    val recordCacheId = Option(allTags.remove("record-cache-id")).map("/" + _).getOrElse("")

    val otherTagsStr = createOtherTagsStr(metric, allTags)

    component + taskId + rocksDbStateId + rocksDbWindowId + rocksDbWindowStateId + inMemWindowId + inMemoryStateId +
      recordCacheId + topic + partition + otherTagsStr + nodeId + processorNodeId + "/" + metricName
  }

  private def isDebugMetric(metricName: MetricName): Boolean = {
    KafkaStreamsFinagleMetricsReporter.debugMetrics.contains(metricName.name)
  }

  private def isGlobalTableMetric(metricName: MetricName): Boolean = {
    val clientId = metricName.tags.get("client-id")
    val threadId = metricName.tags.get("thread-id")
    if (clientId != null) {
      KafkaStreamsFinagleMetricsReporter.globalTableClientIdPatterns.exists(clientId.contains)
    } else if (threadId != null) {
      KafkaStreamsFinagleMetricsReporter.globalTableClientIdPatterns.exists(threadId.contains)
    } else {
      false
    }
  }
}
