package com.twitter.inject.tests

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.inject.InMemoryStatsReceiverUtility
import com.twitter.inject.Test
import com.twitter.conversions.DurationOps._
import com.twitter.util.FuturePool
import org.scalatest.exceptions.TestFailedDueToTimeoutException

class InMemoryStatsReceiverUtilityTest extends Test {
  test("InMemoryStatsReceiverUtility#waitFor will fail if the metric is the incorrect value") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val statUtils = new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)
    // A short timeout is set because the metric will never reach the expected value
    val timeout = 10.millis

    inMemoryStatsReceiver.counter("counter").incr()
    intercept[TestFailedDueToTimeoutException] {
      statUtils.counters.waitFor("counter", 2, timeout)
    }

    inMemoryStatsReceiver.stats(Seq("stats")) = Seq(1.0f, 2.0f, 3.0f)
    intercept[TestFailedDueToTimeoutException] {
      statUtils.stats.waitFor("stats", Seq(1.0f), timeout)
    }

    inMemoryStatsReceiver.addGauge("gauge") { 1.0f }
    intercept[TestFailedDueToTimeoutException] {
      statUtils.gauges.waitFor("gauge", 2.0f, timeout)
    }
  }

  test("InMemoryStatsReceiverUtility#waitFor will fail if metric does not have a declared value") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val statUtils = new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)
    // A short timeout is set because the metric does not have a declared value
    val timeout = 20.millis

    inMemoryStatsReceiver.stats(Seq("stats")) = Seq()
    intercept[TestFailedDueToTimeoutException] {
      statUtils.stats.waitFor("stats", Seq(1.0f), timeout)
    }
  }

  test(
    "InMemoryStatsReceiverUtility#waitFor will accept metrics that are already the expected value") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val statUtils = new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)
    val timeout = 20.millis

    inMemoryStatsReceiver.counter("counter").incr(2)
    statUtils.counters.waitFor("counter", 2, timeout)

    inMemoryStatsReceiver.stats(Seq("stat")) = Seq(1.0f, 2.0f, 3.0f)
    statUtils.stats.waitFor("stat", Seq(1.0f, 2.0f, 3.0f), timeout)

    inMemoryStatsReceiver.addGauge("gauge") { 2.0f }
    statUtils.gauges.waitFor("gauge", 2.0f, timeout)
  }

  test(
    "InMemoryStatsReceiverUtility#waitFor will succeed when metric changes to the expected value within the timeout window") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val sr = new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)
    val timeout = 100.millis

    val counter = inMemoryStatsReceiver.counter("counter")
    val stat = inMemoryStatsReceiver.stat("stat")
    var gaugeValue: Float = 0.0f
    val gauge = inMemoryStatsReceiver.addGauge("gauge") { gaugeValue }

    val counterWait = FuturePool.unboundedPool { sr.counters.waitFor("counter", 1L, timeout) }
    // Change metrics every 20 millis
    FuturePool.unboundedPool {
      for (_ <- 0 to 2) {
        Thread.sleep(20)
        // counter values: 0L, 1L, 2L, 3L
        counter.incr()
      }
    }
    await(counterWait)

    val statsWait = FuturePool.unboundedPool { sr.stats.waitFor("stat", Seq(0.0f, 1.0f), timeout) }
    FuturePool.unboundedPool {
      for (s <- 0 to 2) {
        Thread.sleep(20)
        // histogram values: Seq(0.0f, 1.0f, 2.0f)
        stat.add(s)
      }
    }
    await(statsWait)

    val gaugeWait = FuturePool.unboundedPool { sr.gauges.waitFor("gauge", 1.0f, timeout) }
    FuturePool.unboundedPool {
      for (g <- 0 to 2) {
        Thread.sleep(20)
        // gauge values: 0.0f, 1.0f, 2.0f
        gaugeValue = g
      }
    }
    await(gaugeWait)
  }
}
