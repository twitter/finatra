package com.twitter.finatra.kafka.stats

import com.twitter.conversions.StringOps._
import com.twitter.finagle.stats.Gauge
import com.twitter.finagle.stats.LoadedStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.util.logging.Logging
import java.util
import java.util.regex.Pattern
import org.apache.kafka.common.metrics.KafkaMetric
import org.apache.kafka.common.metrics.MetricsReporter
import scala.collection.JavaConverters._
import scala.collection.mutable

object KafkaFinagleMetricsReporter {

  private[kafka] val IncludeNodeMetrics: String = "include.node.metrics"
  private[kafka] val IncludePartitionMetrics: String = "include.partition.metrics"
  private[kafka] val IncludePartition: String = "includePartition"

  //Hack to allow tests to use an injected StatsReceiver suitable for assertions
  private var globalStatsReceiver: StatsReceiver = LoadedStatsReceiver

  def init(injector: Injector): Unit = {
    globalStatsReceiver = injector.instance[StatsReceiver]
  }

  def sanitizeMetricName(metricName: String): String = {
    KafkaFinagleMetricsReporter.notAllowedMetricPattern
      .matcher(metricName)
      .replaceAll("_")
  }

  private val notAllowedMetricPattern: Pattern =
    Pattern.compile("-| -> |: |, |\\(|\\)| |[^\\w\\d]&&[^./]")

  private val rateMetricsToIgnore: Set[String] = Set(
    "batch-split-rate",
    "buffer-exhausted-rate",
    "byte-rate",
    "bytes-consumed-rate",
    "connection-close-rate",
    "connection-creation-rate",
    "failed-authentication-rate",
    "fetch-rate",
    "heartbeat-rate",
    "incoming-byte-rate",
    "join-rate",
    "network-io-rate",
    "outgoing-byte-rate",
    "record-error-rate",
    "record-retry-rate",
    "record-send-rate",
    "records-consumed-rate",
    "request-rate",
    "response-rate",
    "select-rate",
    "sync-rate",
    "successful-authentication-rate"
  )
}

class KafkaFinagleMetricsReporter extends MetricsReporter with Logging {
  private var statsReceiver: StatsReceiver = _
  private val gauges: mutable.Map[String, Gauge] = mutable.Map()
  private var statsScope: String = ""
  private var includeNodeMetrics: Boolean = _
  private var includePartition: Boolean = _
  private var includePartitionMetrics: Boolean = _

  /* Public */

  override def init(metrics: util.List[KafkaMetric]): Unit = {
    // Initial testing shows that no metrics appear to be passed into init...
  }

  override def configure(configs: util.Map[String, _]): Unit = {
    trace("Configure: " + configs.asScala.mkString("\n"))
    statsScope = Option(configs.get("stats_scope")).getOrElse("kafka").toString
    includeNodeMetrics = Option(configs.get(KafkaFinagleMetricsReporter.IncludeNodeMetrics))
      .map(_.toString.toBoolean)
      .getOrElse(false)
    includePartition = Option(configs.get(KafkaFinagleMetricsReporter.IncludePartition))
      .map(_.toString.toBoolean)
      .getOrElse(true)
    includePartitionMetrics =
      Option(configs.get(KafkaFinagleMetricsReporter.IncludePartitionMetrics))
        .map(_.toString.toBoolean)
        .getOrElse(true)
    statsReceiver = KafkaFinagleMetricsReporter.globalStatsReceiver.scope(statsScope.toString)
  }

  override def metricRemoval(metric: KafkaMetric): Unit = {
    if (shouldIncludeMetric(metric)) {
      val combinedName = createAndSanitizeFinagleMetricName(metric)
      trace("metricRemoval: " + metric.metricName() + "\t" + combinedName)

      for (removedGauge <- gauges.remove(combinedName)) {
        removedGauge.remove()
      }
    }
  }

  override def metricChange(metric: KafkaMetric): Unit = {
    if (shouldIncludeMetric(metric)) {
      val combinedName = createAndSanitizeFinagleMetricName(metric)
      trace("metricChange:  " + metric.metricName() + "\t" + combinedName)

      // Ensure prior metrics are removed (although these should be removed in the metricRemoval method
      for (removedGauge <- gauges.remove(combinedName)) {
        warn(
          s"Duplicate metric found. Removing prior gauges for: " + metric
            .metricName() + "\t" + combinedName
        )
        removedGauge.remove()
      }

      val gauge = statsReceiver.addGauge(combinedName) { metricToFloat(metric) }

      gauges.put(combinedName, gauge)
    }
  }

  override def close(): Unit = {
    trace("Closing FinagleMetricsReporter")
    gauges.values.foreach(_.remove())
    gauges.clear()
  }

  /* Protected */

  protected def createFinagleMetricName(metric: KafkaMetric): String = {
    val allTags = new util.HashMap[String, String]()
    allTags.putAll(metric.metricName().tags())
    allTags.putAll(metric.config().tags())

    val metricName = metric.metricName().name()
    val component =
      parseComponent(clientId = allTags.remove("client-id"), group = metric.metricName().group)
    val nodeId = Option(allTags.remove("node-id")).map("/" + _).getOrElse("")
    val topic = Option(allTags.remove("topic")).map("/" + _).getOrElse("")

    createFinagleMetricName(metric, metricName, allTags, component, nodeId, topic)
  }

  protected def createFinagleMetricName(
    metric: KafkaMetric,
    metricName: String,
    allTags: java.util.Map[String, String],
    component: String,
    nodeId: String,
    topic: String
  ): String = {
    val partition = parsePartitionTag(allTags)
    val otherTagsStr = createOtherTagsStr(metric, allTags)

    component + topic + partition + otherTagsStr + nodeId + "/" + metricName
  }

  protected def createOtherTagsStr(
    metric: KafkaMetric,
    allTags: util.Map[String, String]
  ): String = {
    val otherTagsStr = allTags.asScala.mkString("__").toOption.map("/" + _).getOrElse("")
    if (otherTagsStr.nonEmpty) {
      warn(s"Unexpected metrics tags found: $metric ${metric.metricName()} $otherTagsStr")
    }
    otherTagsStr
  }

  protected def shouldIncludeMetric(metric: KafkaMetric): Boolean = {
    val metricName = metric.metricName()

    // remove any metrics that are already "rated" as these not consistent with other metrics: go/jira/DINS-2187
    if (KafkaFinagleMetricsReporter.rateMetricsToIgnore(metricName.name())) {
      false
    } else if (metricName
        .name() == "assigned-partitions") { //See: https://issues.apache.org/jira/browse/KAFKA-4950 where an occasional error reading the assigned-partitions stat then leads to the instance hanging and not restarting
      false
    } else if (metricName.group
        .contains("node")) { //By default we omit node level metrics which leads to lots of fine grained stats
      includeNodeMetrics
    } else if (metricName
        .tags()
        .containsKey("partition")) { // per partition metrics can explode the metrics namespace
      includePartitionMetrics
    } else {
      metricName.group() != "kafka-metrics-count" &&
      metric.metricValue().isInstanceOf[Number]
    }
  }

  protected def parseComponent(clientId: String, group: String): String = {
    clientId
  }

  protected def parsePartitionTag(allTags: util.Map[String, String]): String = {
    val partitionOpt = Option(allTags.remove("partition"))
    if (!includePartition) {
      ""
    } else {
      partitionOpt.map("/" + _).getOrElse("")
    }
  }

  /* Private */

  private def createAndSanitizeFinagleMetricName(metric: KafkaMetric): String = {
    trace(metric.metricName())
    val finagleMetricName = createFinagleMetricName(metric)
    KafkaFinagleMetricsReporter.sanitizeMetricName(finagleMetricName)
  }

  //Note: We map Double.NegInfinitiy to Float.MinValue since it would otherwise map to Float.NegInfiniti which doesn't render as a number in /admin/metrics.json
  private def metricToFloat(metric: KafkaMetric) = {
    metric.metricValue() match {
      case number: Number if number.doubleValue().isNegInfinity => Float.MinValue
      case number: Number if number.doubleValue().isInfinity => Float.MaxValue
      case number: Number => number.floatValue()
      case _ => Float.NaN
    }
  }
}
