package com.twitter.finatra.kafka.test.utils

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.InMemoryStatsReceiverUtility
import com.twitter.inject.Injector
import com.twitter.util.logging.Logging
import org.scalatest.matchers.should.Matchers

@deprecated("Use com.twitter.inject.InMemoryStatsReceiverUtility", "2019-05-17")
object InMemoryStatsUtil {
  def apply(injector: Injector): InMemoryStatsUtil = {
    val inMemoryStatsReceiver = injector.instance[StatsReceiver].asInstanceOf[InMemoryStatsReceiver]
    new InMemoryStatsUtil(inMemoryStatsReceiver)
  }
}

@deprecated("Use com.twitter.inject.InMemoryStatsReceiverUtility", "2019-05-17")
class InMemoryStatsUtil(val inMemoryStatsReceiver: InMemoryStatsReceiver)
    extends Logging
    with Matchers {

  /** this class now proxies to the InMemoryStatsReceiverUtility */
  private[this] val underlying: InMemoryStatsReceiverUtility =
    new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)

  def statsMap: Map[String, Seq[Float]] =
    underlying.stats.toSortedMap.toMap

  def countersMap: Map[String, Long] =
    underlying.counters.toSortedMap.toMap

  def gaugeMap: Map[String, () => Float] =
    underlying.gauges.toSortedMap.mapValues(() => _).toMap

  def metricNames: Set[String] =
    underlying.names.toSet

  def getCounter(name: String): Long =
    underlying.counters(name)

  def getOptionalCounter(name: String): Option[Long] =
    underlying.counters.get(name)

  def assertCounter(name: String, expected: Long): Unit =
    underlying.counters.assert(name, expected)

  def assertCounter(name: String)(callback: Long => Boolean): Unit =
    underlying.counters.assert(name)(callback)

  def getStat(name: String): Seq[Float] = underlying.stats(name)

  def assertStat(name: String, expected: Seq[Float]): Unit =
    underlying.stats.assert(name, expected)

  def getGauge(name: String): Float =
    underlying.gauges(name)

  def getOptionalGauge(name: String): Option[Float] =
    underlying.gauges.get(name)

  def assertGauge(name: String, expected: Float): Unit =
    underlying.gauges.assert(name, expected)

  def printStats(): Unit = underlying.print()

  def waitForGauge(name: String, expected: Float): Unit =
    underlying.gauges.waitFor(name, expected)

  def waitForGaugeUntil(name: String, predicate: Float => Boolean): Unit =
    underlying.gauges.waitFor(name)(predicate)

  /**
   * Wait for a counter's value to equal the expected
   * @param name Counter name
   * @param expected Expected value of counter
   * @param failIfActualOverExpected Enable to have test fail fast once the current value is greater than the expected value
   * @return
   */
  @deprecated(
    "Use `InMemoryStatsReceiverUtility#counters#waitFor`. The `failIfActualOverExpected` param is no longer ever used here.",
    "2019-05-17"
  )
  def waitForCounter(
    name: String,
    expected: Long,
    failIfActualOverExpected: Boolean = true
  ): Unit =
    underlying.counters.waitFor(name, 60.seconds)(
      _ == expected
    ) // waits up to 60 seconds as per default from PollUtils
}
