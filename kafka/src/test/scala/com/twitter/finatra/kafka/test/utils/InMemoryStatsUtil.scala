package com.twitter.finatra.kafka.test.utils

import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.inject.conversions.map._
import com.twitter.inject.{Injector, Logging}
import org.scalatest.Matchers

object InMemoryStatsUtil {
  def apply(injector: Injector): InMemoryStatsUtil = {
    val inMemoryStatsReceiver = injector.instance[StatsReceiver].asInstanceOf[InMemoryStatsReceiver]
    new InMemoryStatsUtil(inMemoryStatsReceiver)
  }
}

class InMemoryStatsUtil(val inMemoryStatsReceiver: InMemoryStatsReceiver)
    extends Logging
    with Matchers {

  def statsMap: Map[String, Seq[Float]] = inMemoryStatsReceiver.stats.toMap.mapKeys(keyStr)

  def countersMap: Map[String, Long] = inMemoryStatsReceiver.counters.iterator.toMap.mapKeys(keyStr)

  def gaugeMap: Map[String, () => Float] =
    inMemoryStatsReceiver.gauges.iterator.toMap.mapKeys(keyStr)

  def metricNames: Set[String] =
    statsMap.keySet ++ countersMap.keySet ++ gaugeMap.keySet

  def getCounter(name: String): Long = {
    getOptionalCounter(name) getOrElse {
      printStats()
      throw new Exception(name + " not found")
    }
  }

  def getOptionalCounter(name: String): Option[Long] = {
    countersMap.get(name)
  }

  def assertCounter(name: String, expected: Long): Unit = {
    val value = getCounter(name)
    if (value != expected) {
      printStats()
    }
    value should equal(expected)
  }

  def assertCounter(name: String)(callback: Long => Boolean): Unit = {
    callback(getCounter(name)) should be(true)
  }

  def getStat(name: String): Seq[Float] = {
    statsMap.getOrElse(name, throw new Exception(name + " not found"))
  }

  def assertStat(name: String, expected: Seq[Float]): Unit = {
    val value = getStat(name)
    if (value != expected) {
      printStats()
    }
    value should equal(expected)
  }

  def getGauge(name: String): Float = {
    getOptionalGauge(name) getOrElse (throw new Exception(name + " not found"))
  }

  def getOptionalGauge(name: String): Option[Float] = {
    gaugeMap.get(name) map { _.apply() }
  }

  def assertGauge(name: String, expected: Float): Unit = {
    val value = getGauge(name)
    if (value != expected) {
      printStats()
    }
    assert(value == expected)
  }

  def printStats(): Unit = {
    info(" Stats")
    for ((key, values) <- statsMap.toSortedMap) {
      val avg = values.sum / values.size
      val valuesStr = values.mkString("[", ", ", "]")
      info(f"$key%-70s = $avg = $valuesStr")
    }

    info("\nCounters:")
    for ((key, value) <- countersMap.toSortedMap) {
      info(f"$key%-70s = $value")
    }

    info("\nGauges:")
    for ((key, value) <- gaugeMap.toSortedMap) {
      info(f"$key%-70s = ${value()}")
    }
  }

  def waitForGauge(name: String, expected: Float): Unit = {
    waitForGaugeUntil(name, _ == expected)
  }

  def waitForGaugeUntil(name: String, predicate: Float => Boolean): Unit = {
    PollUtils
      .poll[Option[Float]](func = getOptionalGauge(name), exhaustedTriesMessage = result => {
        printStats()
        s"Gauge $name $result did not satisfy predicate"
      })(until = { result =>
        result.nonEmpty && predicate(result.get)
      }).get
  }

  /**
   * Wait for a counter's value to equal the expected
   * @param name Counter name
   * @param expected Expected value of counter
   * @param failIfActualOverExpected Enable to have test fail fast once the current value is greater than the expected value
   * @return
   */
  def waitForCounter(
    name: String,
    expected: Long,
    failIfActualOverExpected: Boolean = true
  ): Unit = {
    PollUtils
      .poll[Option[Long]](func = getOptionalCounter(name), exhaustedTriesMessage = result => {
        printStats()
        s"Counter $name $result != $expected"
      })(until = { result =>
        if (failIfActualOverExpected && result.isDefined) {
          assert(result.get <= expected, "Actual counter value is greater than the expected value")
        }
        result.getOrElse(0) == expected
      })
  }

  def waitForCounterUntil(name: String, predicate: Long => Boolean): Long = {
    PollUtils
      .poll[Option[Long]](func = getOptionalCounter(name), exhaustedTriesMessage = result => {
        printStats()
        s"Counter $name $result did not satisfy predicate"
      })(until = { result =>
        result.nonEmpty && predicate(result.get)
      }).get
  }

  private def keyStr(keys: Seq[String]): String = {
    keys.mkString("/")
  }
}
