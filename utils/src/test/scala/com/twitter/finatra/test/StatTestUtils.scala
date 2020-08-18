package com.twitter.finatra.test

import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.scalatest.matchers.should.Matchers

object StatTestUtils extends Matchers {

  def clear(statsReceiver: InMemoryStatsReceiver): Unit = {
    statsReceiver.counters.clear()
    statsReceiver.stats.clear()
    statsReceiver.gauges.clear()
  }

  def assertCounter(
    statsReceiver: InMemoryStatsReceiver,
    name: String,
    expectedValue: Long
  ): Unit = {
    val actualValue = statsReceiver.counters.getOrElse(Seq(name), 0L)
    actualValue should equal(expectedValue)
  }

  def assertGauge(
    statsReceiver: InMemoryStatsReceiver,
    name: String,
    expectedValue: Float
  ): Unit = {
    val actualValue = statsReceiver.gauges.getOrElse(Seq(name), () => 0f)
    if (expectedValue != actualValue()) {
      println("Failure asserting " + name)
      actualValue() should equal(expectedValue)
    }
  }

  def printStatsAndCounters(statsReceiver: InMemoryStatsReceiver): Unit = {
    def pretty(map: Iterator[(Seq[String], Any)]): Unit = {
      for ((keys, value) <- map) {
        println(keys.mkString("/") + " = " + value)
      }
    }

    pretty(statsReceiver.stats.iterator)
    pretty(statsReceiver.counters.iterator)
    pretty(statsReceiver.gauges.iterator)
  }

  def pretty(map: Iterator[(Seq[String], Any)]): Unit = {
    for ((keys, value) <- map) {
      println(keys.mkString("/") + " = " + value)
    }
  }

  def printStats(statsReceiver: InMemoryStatsReceiver): Unit = {
    val stats = statsReceiver.stats.map {
      case (keys, values) =>
        keys.mkString("/") -> values.mkString(", ")
    }.toSeq

    val counters = statsReceiver.counters.map {
      case (keys, valueInt) =>
        keys.mkString("/") -> valueInt
    }.toSeq

    val gauges = statsReceiver.gauges.map {
      case (keys, intFunc) =>
        keys.mkString("/") -> intFunc()
    }.toSeq

    for ((key, value) <- (stats ++ counters ++ gauges).sortBy(_._1)) {
      println("%-75s = %s".format(key, ellipses(value, 60)))
    }
    println()
  }

  private def ellipses(any: Any, max: Int) = {
    val str = any.toString
    if (str.length > max)
      str.take(max) + "..."
    else
      str
  }

}
