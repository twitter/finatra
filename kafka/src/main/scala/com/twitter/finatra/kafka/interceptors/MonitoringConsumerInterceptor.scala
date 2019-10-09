package com.twitter.finatra.kafka.interceptors

import com.google.common.primitives.Longs
import com.twitter.finatra.kafka.interceptors.PublishTimeProducerInterceptor._
import com.twitter.finagle.stats.{LoadedStatsReceiver, Stat, StatsReceiver}
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafka.utils.ConfigUtils
import com.twitter.inject.Injector
import com.twitter.util.Time
import java.util
import org.apache.kafka.clients.consumer.{ConsumerInterceptor, ConsumerRecords, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition
import scala.collection.mutable

object MonitoringConsumerInterceptor {
  private var globalStatsReceiver: StatsReceiver = LoadedStatsReceiver

  def init(injector: Injector): Unit = {
    globalStatsReceiver = injector.instance[StatsReceiver]
  }
}

/**
 * An interceptor that looks for the `publish_time` header and record timestamp and calculates
 * how much time has passed since the each of those times and updates stats for each.
 */
class MonitoringConsumerInterceptor extends ConsumerInterceptor[Any, Any] {

  private var consumerStatsReceiver: StatsReceiver = _
  private val topicNameToLagStat = mutable.Map[TopicAndStatName, Stat]()
  private var enabled: Boolean = _

  override def configure(configs: util.Map[String, _]): Unit = {
    val consumerClientId = ConfigUtils.getConfigOrElse(configs, "client.id", "")
    enabled = enableInterceptorForClientId(consumerClientId)

    val statsScope = ConfigUtils.getConfigOrElse(configs, key = "stats_scope", default = "kafka")
    consumerStatsReceiver = MonitoringConsumerInterceptor.globalStatsReceiver
      .scope(statsScope)
      .scope("consumer")
  }

  override def onConsume(records: ConsumerRecords[Any, Any]): ConsumerRecords[Any, Any] = {
    if (enabled) {
      val now = Time.now.inMillis
      val iterator = records.iterator()
      while (iterator.hasNext) {
        val record = iterator.next()
        val topic = record.topic()

        val publishTimeHeader = record.headers().lastHeader(PublishTimeHeaderName)
        if (publishTimeHeader != null) {
          val publishTimeHeaderMillis = Longs.fromByteArray(publishTimeHeader.value())
          updateLagStat(
            now = now,
            topic = topic,
            timestamp = publishTimeHeaderMillis,
            statName = "time_since_record_published_ms"
          )
        }

        val recordTimestamp = record.timestamp()
        updateLagStat(
          now = now,
          topic = topic,
          timestamp = recordTimestamp,
          statName = "time_since_record_timestamp_ms"
        )
      }
    }

    records
  }

  override def onCommit(offsets: util.Map[TopicPartition, OffsetAndMetadata]): Unit = {}

  override def close(): Unit = {
    topicNameToLagStat.clear()
  }

  /**
   * Determines if this interceptor should be enabled given the consumer client id
   */
  protected def enableInterceptorForClientId(consumerClientId: String): Boolean = {
    true
  }

  /* Private */

  private def createNewStat(topicName: String, statName: String): Stat = {
    consumerStatsReceiver
      .scope(KafkaFinagleMetricsReporter.sanitizeMetricName(topicName))
      .stat(statName)
  }

  //TODO: Optimize map lookup which is a hotspot during profiling
  private def updateLagStat(now: Long, topic: String, timestamp: Long, statName: String): Unit = {
    val lag = now - timestamp
    if (lag >= 0) {
      val cacheKey = TopicAndStatName(topic, statName)
      val lagStat = topicNameToLagStat.getOrElseUpdate(
        cacheKey,
        createNewStat(topicName = topic, statName = statName)
      )
      lagStat.add(lag)
    }
  }

  private case class TopicAndStatName(topic: String, statName: String)
}
