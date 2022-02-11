package com.twitter.finatra.kafkastreams.internal.stats

import com.twitter.finagle.stats.Gauge
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.logging.Logging
import java.util.concurrent.atomic.AtomicLong
import org.rocksdb.HistogramData
import org.rocksdb.HistogramType
import org.rocksdb.StatisticsCollectorCallback
import org.rocksdb.TickerType
import scala.collection.mutable.{Map => MutableMap}

/**
 * Implements the callback statistics collection and reporting for RocksDB Statistics.
 *
 * All ticker stats are cumulative since process start.
 * Histograms measures distribution of a stat across all operations.
 *
 * Stats are scoped to "rocksdb/statistics".
 *
 * For more information see:
 *   https://github.com/facebook/rocksdb/wiki/Statistics
 *   https://github.com/facebook/rocksdb/blob/master/include/rocksdb/statistics.h
 */
private[kafkastreams] class RocksDBStatsCallback(statsReceiver: StatsReceiver)
    extends StatisticsCollectorCallback
    with Logging {

  /**
   * Root scope for statistics.
   */
  private val statisticsStatsScope: StatsReceiver = statsReceiver.scope("rocksdb", "statistics")

  /**
   * List of ignored ticker types that will not be included in exported stats.
   */
  private val ignoredTickerTypes: Seq[TickerType] = Seq(
    TickerType.STALL_L0_SLOWDOWN_MICROS,
    TickerType.STALL_MEMTABLE_COMPACTION_MICROS,
    TickerType.STALL_L0_NUM_FILES_MICROS,
    TickerType.TICKER_ENUM_MAX
  )

  /**
   * List of ignored histogram types that will not be included in exported stats.
   */
  private val ignoredHistogramTypes: Seq[HistogramType] = Seq(
    HistogramType.HISTOGRAM_ENUM_MAX
  )

  /**
   * Allowed ticker types used for stats collection.
   */
  private val allowedTickerTypes: Seq[TickerType] = TickerType.values.filter(isAllowedTickerType)

  /**
   * Allowed histogram types used for stats collection.
   */
  private val allowedHistogramTypes: Seq[HistogramType] =
    HistogramType.values.filter(isAllowedHistogramType)

  /**
   * Most recent counter values from TickerType to Value.
   */
  private val mostRecentCounterValues: Map[TickerType, AtomicLong] = allowedTickerTypes.map {
    tickerType =>
      tickerType -> new AtomicLong(0)
  }.toMap

  /**
   * Ticker gauges. Note we need to hold a strong reference to prevent GC from collecting away the gauge.
   */
  private val tickerGauges = allowedTickerTypes.map { tickerType =>
    statisticsStatsScope.addGauge(tickerTypeName(tickerType)) {
      mostRecentCounterValues.get(tickerType).map(_.floatValue()).getOrElse(0f)
    }
  }

  /**
   * Updatable histogram cache.
   */
  private val histogramCache: MutableMap[String, Float] = MutableMap.empty[String, Float]

  /**
   * Histogram suffixes
   */
  private val histogramSuffixes = Seq("avg", "std", "p50", "p95", "p99")

  /**
   * Histogram gauges. Note we need to hold a strong reference to prevent GC from collecting away the gauge.
   */
  private val histogramGauges: Seq[Gauge] = allowedHistogramTypes.flatMap { histogramType =>
    val prefix = histogramTypeName(histogramType) + "_"
    for {
      suffix <- histogramSuffixes
    } yield {
      // NOTE: We use Gauge here because these metrics are coming out of RocksDB already as percentiles:
      // we don't have the raw data. Hence we use Gauge which will avoid a percentile-of-percentile
      // that we'll get if we used Metric.
      val statName = prefix + suffix
      statisticsStatsScope.addGauge(statName) {
        histogramCache.getOrElse(statName, 0f)
      }
    }
  }

  /**
   * Callback that updates counters by ticker count for given ticker type
   */
  override def tickerCallback(tickerType: TickerType, tickerCount: Long): Unit = {
    if (isAllowedTickerType(tickerType)) {
      mostRecentCounterValues.get(tickerType).foreach(_.set(tickerCount))
    }
  }

  /**
   * Callback that updates histogram cache for gauges
   */
  override def histogramCallback(
    histogramType: HistogramType,
    histogramData: HistogramData
  ): Unit = {
    if (isAllowedHistogramType(histogramType)) {
      val prefix = histogramTypeName(histogramType) + "_"
      histogramCache.update(prefix + "avg", histogramData.getAverage.toFloat)
      histogramCache.update(prefix + "std", histogramData.getStandardDeviation.toFloat)
      histogramCache.update(prefix + "p50", histogramData.getMedian.toFloat)
      histogramCache.update(prefix + "p95", histogramData.getPercentile95.toFloat)
      histogramCache.update(prefix + "p99", histogramData.getPercentile99.toFloat)
    }
  }

  /**
   * Accessor for this callback's ticker values.
   */
  def tickerValues: Map[TickerType, Long] = {
    mostRecentCounterValues.mapValues(_.get()).toMap
  }

  /**
   * Simplified ticker name.
   */
  private def tickerTypeName(tickerType: TickerType): String = {
    tickerType.name().toLowerCase
  }

  /**
   * Simplified histogram name.
   */
  private def histogramTypeName(histogramType: HistogramType): String = {
    histogramType.name().toLowerCase
  }

  /**
   * Returns true if ticker type is allowed to be included in stats, false otherwise.
   */
  private def isAllowedTickerType(tickerType: TickerType): Boolean = {
    !ignoredTickerTypes.contains(tickerType)
  }

  /**
   * Returns true if histogram type is allowed to be included in stats, false otherwise.
   */
  private def isAllowedHistogramType(histogramType: HistogramType): Boolean = {
    !ignoredHistogramTypes.contains(histogramType)
  }
}
