package com.twitter.finatra.test

import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.scalatest.Matchers

object StatTestUtils extends Matchers {

  def clear(statsReceiver: InMemoryStatsReceiver) {
    statsReceiver.counters.clear()
    statsReceiver.stats.clear()
    statsReceiver.gauges.clear()
  }

  def assertCounter(
    statsReceiver: InMemoryStatsReceiver,
    name: String,
    expectedValue: Int) {
    val actualValue = statsReceiver.counters.get(Seq(name)) getOrElse 0
    actualValue should equal(expectedValue)
  }

  def assertGauge(
    statsReceiver: InMemoryStatsReceiver,
    name: String,
    expectedValue: Float) {
    val actualValue = statsReceiver.gauges.get(Seq(name)).getOrElse(() => 0f)
    if (expectedValue != actualValue()) {
      println("Failure asserting " + name)
      actualValue() should equal(expectedValue)
    }
  }

  def printStatsAndCounters(statsReceiver: InMemoryStatsReceiver) {
    def pretty(map: Iterator[(Seq[String], Any)]) = {
      for ((keys, value) <- map) {
        println(
          keys.mkString("/") + " = " + value)
      }
    }

    pretty(statsReceiver.stats.iterator)
    pretty(statsReceiver.counters.iterator)
    pretty(statsReceiver.gauges.iterator)
  }

  def pretty(map: Iterator[(Seq[String], Any)]) = {
    for ((keys, value) <- map) {
      println(
        keys.mkString("/") + " = " + value)
    }
  }

  def printStats(statsReceiver: InMemoryStatsReceiver) {
    val stats = statsReceiver.stats.map { case (keys, values) =>
      keys.mkString("/") -> values.mkString(", ")
    }.toSeq

    val counters = statsReceiver.counters.map { case (keys, valueInt) =>
      keys.mkString("/") -> valueInt
    }.toSeq

    val gauges = statsReceiver.gauges.map { case (keys, intFunc) =>
      keys.mkString("/") -> intFunc()
    }.toSeq

    for ((key, value) <- (stats ++ counters ++ gauges).sortBy(_._1)) {
      println("%-75s = %s".format(key, ellipses(value, 60)))
    }
    println()
  }

  private def ellipses(any: Any, max: Int) = {
    val str = any.toString
    if (str.size > max)
      str.take(max) + "..."
    else
      str
  }

}
