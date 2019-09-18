package com.twitter.inject.server

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.inject.Injector
import com.twitter.inject.conversions.map._
import com.twitter.util.Duration
import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import org.scalatest.{Assertion, Matchers}
import org.scalatest.concurrent.{Eventually, PatienceConfiguration}
import org.scalatest.time.{Millis, Span}
import scala.collection.{SortedMap, SortedSet}
import scala.collection.JavaConverters._

object InMemoryStatsReceiverUtility {
  def apply(injector: Injector): InMemoryStatsReceiverUtility = {
    injector.instance[StatsReceiver] match {
      case receiver: InMemoryStatsReceiver =>
        new InMemoryStatsReceiverUtility(receiver)
      case _ =>
        throw new IllegalArgumentException(
          "A StatsReceiver of type `InMemoryStatsReceiver` was not bound to the provided injector.")
    }
  }
}

/**
 * A utility for working with [[com.twitter.finagle.stats.Counter]]s, [[com.twitter.finagle.stats.Stat]]s
 * and [[com.twitter.finagle.stats.Gauge]]s from an instance of an [[InMemoryStatsReceiver]]. This
 * class allows users to query for a given metric, assert values, or to "wait" for a metric value.
 *
 * @param inMemoryStatsReceiver the underlying [[InMemoryStatsReceiver]].
 *
 * @see [[com.twitter.finagle.stats.InMemoryStatsReceiver]]
 * @see [[com.twitter.finagle.stats.Counter]]
 * @see [[com.twitter.finagle.stats.Stat]]
 * @see [[com.twitter.finagle.stats.Gauge]]
 */
class InMemoryStatsReceiverUtility(inMemoryStatsReceiver: InMemoryStatsReceiver)
    extends Matchers
    with Eventually {

  class Counters private[InMemoryStatsReceiverUtility] () extends InMemoryStats[Long] {

    /**
     * Returns a sorted `Map` of collected [[com.twitter.finagle.stats.Counter]] values from the
     * underlying [[InMemoryStatsReceiver]]. The map is sorted by the natural ordering of the
     * `String` key which represents the [[com.twitter.finagle.stats.Counter]] name.
     *
     * @return a [[SortedMap]] of [[com.twitter.finagle.stats.Counter]] name to `Long` representing
     *         the current [[com.twitter.finagle.stats.Counter]] value for the given name.
     */
    def toSortedMap: SortedMap[String, Long] =
      inMemoryStatsReceiver.counters.iterator.toMap.mapKeys(keyStr).toSortedMap

    /**
     * A complete sorted `Set` of all [[com.twitter.finagle.stats.Counter]] names collected by
     * the underlying [[InMemoryStatsReceiver]]. The set is sorted by the natural ordering of the
     * `String` [[com.twitter.finagle.stats.Counter]] name.
     *
     * @return a [[SortedSet]] of all the [[com.twitter.finagle.stats.Counter]] names collected
     *         by the underlying [[InMemoryStatsReceiver]].
     */
    def names: SortedSet[String] = this.toSortedMap.keySet

    /**
     * Returns an `Option[Long]` value for the [[com.twitter.finagle.stats.Counter]] collected with
     * the given name. If a [[com.twitter.finagle.stats.Counter]] exists with the given name, then
     * `Some(value)` will be returned, otherwise a [[None]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Counter]].
     *
     * @return the current value of the [[com.twitter.finagle.stats.Counter]] wrapped in a [[Some]],
     *         if it exists in the underlying [[InMemoryStatsReceiver]], otherwise [[None]].
     */
    def get(name: String): Option[Long] = this.toSortedMap.get(name)

    /**
     * Returns the [[Long]] value for the [[com.twitter.finagle.stats.Counter]] collected with
     * the given name or an [[IllegalArgumentException]] if the [[com.twitter.finagle.stats.Counter]]
     * does not exist in the underlying [[InMemoryStatsReceiver]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Counter]].
     *
     * @return the current [[Long]] value of the [[com.twitter.finagle.stats.Counter]] if it exists
     *         in the underlying [[InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
     */
    def apply(name: String): Long = this.apply(name, verbose = true)

    /**
     * Returns the [[Long]] value for the [[com.twitter.finagle.stats.Counter]] collected with
     * the given name or an [[IllegalArgumentException]] if the [[com.twitter.finagle.stats.Counter]]
     * does not exist in the underlying [[InMemoryStatsReceiver]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Counter]].
     * @param verbose if the current stats should be printed.
     *
     * @return the current [[Long]] value of the [[com.twitter.finagle.stats.Counter]] if it exists
     *         in the underlying [[InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
     */
    def apply(name: String, verbose: Boolean): Long = this.get(name).getOrElse {
      if (verbose) print()
      throw new IllegalArgumentException(s"""Counter "$name" was not found""")
    }

    /**
     * Asserts that the [[com.twitter.finagle.stats.Counter]] represented by the given name
     * has the given expected value. If the returned lookup value does not match the expected value
     * a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
     *
     * @note If a [[com.twitter.finagle.stats.Counter]] with the given name does not exist an
     * [[IllegalArgumentException]] will be thrown.
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Counter]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param expected the expected value of the [[com.twitter.finagle.stats.Counter]].
     */
    def assert(name: String, expected: Long): Unit =
      this.apply(name) should equal(expected)

    /** For Java compatibility */
    def expect(name: String, expected: Long): Unit =
      this.assert(name, expected)

    /**
     * Asserts that the passed predicate callback function executed on a [[com.twitter.finagle.stats.Counter]]
     * represented by the given name returns `True`. If the executed callback function does not
     * return `true` a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
     *
     * @note If a [[com.twitter.finagle.stats.Counter]] with the given name does not exist an
     *       [[IllegalArgumentException]] will be thrown.
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Counter]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param predicate the function to execute on the looked up [[com.twitter.finagle.stats.Counter]]
     *                 which SHOULD return `True`.
     */
    def assert(name: String)(predicate: Long => Boolean): Unit =
      predicate(this.apply(name)) should be(true)

    /**
     * Waits for the [[com.twitter.finagle.stats.Counter]] represented by the given name to
     * equal the given expected value. If a [[com.twitter.finagle.stats.Counter]] value is not found
     * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
     * will be thrown.
     *
     * @note The default timeout is 150ms. To specify a different timeout, users should use
     *       [[waitFor(name: String, timeout: Duration)(predicate: Long => Boolean)]].
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Counter]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param expected the expected value of the [[com.twitter.finagle.stats.Counter]].
     *
     * @see [[org.scalatest.concurrent.Eventually.eventually]]
     * @see [[waitFor(name: String, predicate: Long => Boolean, timeout: Duration)]]
     */
    def waitFor(name: String, expected: Long): Unit = this.assertCounter(name, expected)

    /**
     * Waits for the [[com.twitter.finagle.stats.Counter]] represented by the given name to
     * equal the given expected value. If a [[com.twitter.finagle.stats.Counter]] value is not found
     * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
     * will be thrown.
     *
     * @param name      the identifier of the [[com.twitter.finagle.stats.Counter]] to lookup
     *                  from the underlying [[InMemoryStatsReceiver]].
     * @param timeout   the [[com.twitter.util.Duration]] to wait for the retrieval of the counter and
     *                  the predicate to return `True`. The default is 150 ms.
     * @param predicate the function to execute on the retrieved [[com.twitter.finagle.stats.Counter]]
     *                  which is expected to return `True`.
     *
     * @see [[org.scalatest.concurrent.Eventually.eventually]]
     */
    def waitFor(name: String, timeout: Duration = 150.millis)(predicate: Long => Boolean): Unit =
      this.assertCounter(name, predicate, timeout)

    private def assertCounter(
      name: String,
      predicate: Long => Boolean,
      timeout: Duration
    ): Assertion = eventually(timeout) {
      val actualValue: Long = this.apply(name, verbose = false)
      withClue(s"""Asserted predicate for counter "$name" never evaluated to true:""") {
        predicate(actualValue) should be(true)
      }
    }

    private def assertCounter(
      name: String,
      expectedValue: Long,
      timeout: Duration = 150.millis
    ): Assertion = eventually(timeout) {
      val actualValue: Long = this.apply(name, verbose = false)
      withClue(s"""Expected "$expectedValue" for counter "$name" but got "$actualValue"""") {
        actualValue should equal(expectedValue)
      }
    }

    private[InMemoryStatsReceiverUtility] def clear(): Unit =
      inMemoryStatsReceiver.counters.clear()
  }

  class Stats private[InMemoryStatsReceiverUtility] () extends InMemoryStats[Seq[Float]] {

    /**
     * Returns a sorted `Map` of collected [[com.twitter.finagle.stats.Stat]] values from the
     * underlying [[InMemoryStatsReceiver]]. The map is sorted by the natural ordering of the
     * `String` key which represents the [[com.twitter.finagle.stats.Stat]] name.
     *
     * @return a [[SortedMap]] of [[com.twitter.finagle.stats.Stat]] name to `Seq[Float]`
     *         representing the [[com.twitter.finagle.stats.Stat]] values collected for
     *         the given name.
     */
    def toSortedMap: SortedMap[String, Seq[Float]] =
      inMemoryStatsReceiver.stats.iterator.toMap.mapKeys(keyStr).toSortedMap

    /**
     * A complete sorted `Set` of all [[com.twitter.finagle.stats.Stat]] names collected by
     * the underlying [[InMemoryStatsReceiver]]. The set is sorted by the natural ordering of the
     * `String` [[com.twitter.finagle.stats.Stat]] name.
     *
     * @return a [[SortedSet]] of all the [[com.twitter.finagle.stats.Stat]] names collected
     *         by the underlying [[InMemoryStatsReceiver]].
     */
    def names: SortedSet[String] = this.toSortedMap.keySet

    /**
     * Returns an `Option[Seq[Float]]` value for the [[com.twitter.finagle.stats.Stat]] collected with
     * the given name. If a [[com.twitter.finagle.stats.Stat]] exists with the given name, then
     * `Some(Seq)` will be returned, otherwise a [[None]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Stat]].
     *
     * @return the current [[Seq]] of values of the [[com.twitter.finagle.stats.Stat]] wrapped in a
     *         [[Some]], if it exists in the underlying [[InMemoryStatsReceiver]], otherwise [[None]].
     */
    def get(name: String): Option[Seq[Float]] = this.toSortedMap.get(name)

    /**
     * Returns the [[Seq]] of values for the [[com.twitter.finagle.stats.Stat]] with the given
     * name or an [[IllegalArgumentException]] if the [[com.twitter.finagle.stats.Stat]] does not
     * exist in the underlying [[InMemoryStatsReceiver]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Stat]].
     *
     * @return the current [[Seq]] of values of the [[com.twitter.finagle.stats.Stat]] if it exists
     *         in the underlying [[InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
     */
    def apply(name: String): Seq[Float] = this.apply(name, verbose = true)

    /**
     * Returns the [[Seq]] of values for the [[com.twitter.finagle.stats.Stat]] with the given
     * name or an [[IllegalArgumentException]] if the [[com.twitter.finagle.stats.Stat]] does not
     * exist in the underlying [[InMemoryStatsReceiver]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Stat]].
     * @param verbose if the current stats should be printed.
     *
     * @return the current [[Seq]] of values of the [[com.twitter.finagle.stats.Stat]] if it exists
     *         in the underlying [[InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
     */
    def apply(name: String, verbose: Boolean): Seq[Float] = this.get(name).getOrElse {
      if (verbose) print()
      throw new IllegalArgumentException(s"""Stat "$name" was not found""")
    }

    /**
     * Asserts that the [[com.twitter.finagle.stats.Stat]] represented by the given name
     * equals the given expected [[Seq]] of values. If the returned [[Seq]] of values does match the
     * expected a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
     *
     * @note If a [[com.twitter.finagle.stats.Stat]] with the given name does not exist an
     *       [[IllegalArgumentException]] will be thrown.
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Stat]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param expected the expected [[Seq]] of values of the [[com.twitter.finagle.stats.Stat]].
     */
    def assert(name: String, expected: Seq[Float]): Unit =
      this.apply(name) should equal(expected)

    /** For Java compatibility */
    override def expect(name: String, expected: Seq[Float]): Unit =
      this.expect(name, expected.asJava)

    /** For Java compatibility */
    def expect(name: String, expected: java.util.Collection[Float]): Unit = {
      this.assert(name, expected.asScala.toSeq)
    }

    /**
     * Asserts that the passed predicate callback function executed on a [[com.twitter.finagle.stats.Stat]]
     * represented by the given name returns `True`. If the executed callback function does not
     * return `true` a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
     *
     * @note If a [[com.twitter.finagle.stats.Stat]] with the given name does not exist an
     *       [[IllegalArgumentException]] will be thrown.
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Stat]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param predicate the function to execute on the looked up [[com.twitter.finagle.stats.Stat]]
     *                 which SHOULD return `True`.
     */
    def assert(name: String)(predicate: Seq[Float] => Boolean): Unit =
      predicate(this.apply(name)) should be(true)

    /**
     * Waits for the [[com.twitter.finagle.stats.Stat]] represented by the given name to
     * equal the given expected value. If a metric value is not found
     * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
     * will be thrown.
     *
     * @note The default timeout is 150ms. To specify a different timeout, users should use
     *       [[waitFor(name: String, timeout: Duration)(predicate: T => Boolean)]].
     * @param name     the identifier of the metric to lookup
     *                 from the underlying [[InMemoryStatsReceiver]].
     * @param expected the expected value of the metric.
     *
     * @see [[org.scalatest.concurrent.Eventually.eventually]]
     * @see [[waitFor(name: String, predicate: Seq[Float] => Boolean, timeout: Duration)]]
     */
    override def waitFor(name: String, expected: Seq[Float]): Unit = this.assertStat(name, expected)

    /**
     * Waits for the metric represented by the given name to
     * equal the given expected value. If a metric value is not found
     * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
     * will be thrown.
     *
     * @param name      the identifier of the metric to lookup
     *                  from the underlying [[InMemoryStatsReceiver]].
     * @param timeout   the [[com.twitter.util.Duration]] to wait for the retrieval of the counter and
     *                  the predicate to return `True`. The default is 150 ms.
     * @param predicate the function to execute on the retrieved metric
     *                  which is expected to return `True`.
     *
     * @see [[org.scalatest.concurrent.Eventually.eventually]]
     */
    override def waitFor(
      name: String,
      timeout: Duration = 150.millis
    )(predicate: Seq[Float] => Boolean
    ): Unit = this.assertStat(name, predicate, timeout)

    private def assertStat(
      name: String,
      predicate: Seq[Float] => Boolean,
      timeout: Duration
    ): Assertion = eventually(timeout) {
      val actualValue: Seq[Float] = this.apply(name, verbose = false)
      withClue(s"""Asserted predicate for stat "$name" never evaluated to true:""") {
        predicate(actualValue) should be(true)
      }
    }

    private def assertStat(
      name: String,
      expectedValue: Seq[Float],
      timeout: Duration = 150.millis
    ): Assertion = eventually(timeout) {
      val actualValue: Seq[Float] = this.apply(name, verbose = false)
      withClue(s"""Expected "$expectedValue" for stat "$name" but got "$actualValue":""") {
        actualValue should equal(expectedValue)
      }
    }

    private[InMemoryStatsReceiverUtility] def clear(): Unit =
      inMemoryStatsReceiver.stats.clear()
  }

  class Gauges private[InMemoryStatsReceiverUtility] () extends InMemoryStats[Float] {

    /**
     * Returns a sorted `Map` of collected [[com.twitter.finagle.stats.Gauge]] values from the
     * underlying [[InMemoryStatsReceiver]]. The map is sorted by the natural ordering of the
     * `String` key which represents the [[com.twitter.finagle.stats.Gauge]] name.
     *
     * @return a [[SortedMap]] of [[com.twitter.finagle.stats.Gauge]] name to a Function of
     *         `Unit => Float` which represents the current [[com.twitter.finagle.stats.Gauge]]
     *         value for the given name.
     */
    def toSortedMap: SortedMap[String, Float] =
      inMemoryStatsReceiver.gauges.iterator.toMap.mapKeys(keyStr).mapValues(_.apply).toSortedMap

    /**
     * A complete sorted `Set` of all [[com.twitter.finagle.stats.Gauge]] names collected by
     * the underlying [[InMemoryStatsReceiver]]. The set is sorted by the natural ordering of the
     * `String` [[com.twitter.finagle.stats.Gauge]] name.
     *
     * @return a [[SortedSet]] of all the [[com.twitter.finagle.stats.Gauge]] names collected
     *         by the underlying [[InMemoryStatsReceiver]].
     */
    def names: SortedSet[String] = this.toSortedMap.keySet

    /**
     * Returns an `Option[Float]` value for the [[com.twitter.finagle.stats.Gauge]] collected with
     * the given name. If a [[com.twitter.finagle.stats.Gauge]] exists with the given name, then
     * `Some(Float)` will be returned, otherwise a [[None]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Gauge]].
     *
     * @return the current [[Float]] value of the [[com.twitter.finagle.stats.Gauge]] wrapped in a
     *         [[Some]], if it exists in the underlying [[InMemoryStatsReceiver]], otherwise [[None]].
     */
    def get(name: String): Option[Float] = this.toSortedMap.get(name)

    /**
     * Returns the [[Float]] value for the [[com.twitter.finagle.stats.Gauge]] collected with the
     * given name or an [[IllegalArgumentException]] if the [[com.twitter.finagle.stats.Gauge]] does
     * not exist in the underlying [[InMemoryStatsReceiver]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Gauge]].
     *
     * @return the current [[Float]] value of the [[com.twitter.finagle.stats.Gauge]] if it exists
     *         in the underlying [[InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
     */
    def apply(name: String): Float = this.apply(name, verbose = true)

    /**
     * Returns the [[Float]] value for the [[com.twitter.finagle.stats.Gauge]] collected with the
     * given name or an [[IllegalArgumentException]] if the [[com.twitter.finagle.stats.Gauge]] does
     * not exist in the underlying [[InMemoryStatsReceiver]].
     *
     * @param name the registered `String` to use as the lookup key for a collected
     *             [[com.twitter.finagle.stats.Gauge]].
     * @param verbose if the current stats should be printed
     *
     * @return the current [[Float]] value of the [[com.twitter.finagle.stats.Gauge]] if it exists
     *         in the underlying [[InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
     */
    def apply(name: String, verbose: Boolean): Float = this.get(name).getOrElse {
      if (verbose) print()
      throw new IllegalArgumentException(s"""Gauge "$name" was not found""")
    }

    /**
     * Asserts that the [[com.twitter.finagle.stats.Gauge]] represented by the given name
     * equals the given expected [[Float]] value. If the returned [[Float]] value does match the
     * expected, a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
     *
     * @note If a [[com.twitter.finagle.stats.Gauge]] with the given name does not exist an
     *       [[IllegalArgumentException]] will be thrown.
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Gauge]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param expected the expected [[Float]] value of the [[com.twitter.finagle.stats.Gauge]].
     */
    def assert(name: String, expected: Float): Unit =
      this.apply(name) should equal(expected)

    /** For Java compatibility */
    def expect(name: String, expected: Float): Unit =
      this.assert(name, expected)

    /**
     * Asserts that the passed predicate callback function executed on a [[com.twitter.finagle.stats.Gauge]]
     * represented by the given name returns `True`. If the executed callback function does not
     * return `true` a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
     *
     * @note If a [[com.twitter.finagle.stats.Gauge]] with the given name does not exist an
     *       [[IllegalArgumentException]] will be thrown.
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Gauge]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param predicate the function to execute on the looked up [[com.twitter.finagle.stats.Gauge]]
     *                 which SHOULD return `True`.
     */
    def assert(name: String)(predicate: Float => Boolean): Unit =
      predicate(this.apply(name)) should be(true)

    /**
     * Waits for the [[com.twitter.finagle.stats.Gauge]] represented by the given name to
     * equal the given expected value. If a [[com.twitter.finagle.stats.Gauge]] value is not found
     * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
     * will be thrown.
     *
     * @note The default timeout is 150ms. To specify a different timeout, users should use
     *       [[waitFor(name: String, timeout: Duration)(predicate: Long => Boolean)]].
     *
     * @param name the identifier of the [[com.twitter.finagle.stats.Gauge]] to lookup
     *             from the underlying [[InMemoryStatsReceiver]].
     * @param expected the expected value of the [[com.twitter.finagle.stats.Gauge]].
     *
     * @see [[org.scalatest.concurrent.Eventually.eventually]]
     * @see [[waitFor(name: String, predicate: Long => Boolean, timeout: Duration)]]
     */
    def waitFor(name: String, expected: Float): Unit = this.assertGauge(name, expected)

    /**
     * Waits for the [[com.twitter.finagle.stats.Gauge]] represented by the given name to
     * equal the given expected value. If a [[com.twitter.finagle.stats.Gauge]] value is not found
     * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
     * will be thrown.
     *
     * @param name      the identifier of the [[com.twitter.finagle.stats.Gauge]] to lookup
     *                  from the underlying [[InMemoryStatsReceiver]].
     * @param timeout   the [[com.twitter.util.Duration]] to wait for the retrieval of the counter and
     *                  the predicate to return `True`. The default is 150 ms.
     * @param predicate the function to execute on the retrieved [[com.twitter.finagle.stats.Gauge]]
     *                  which is expected to return `True`.
     *
     * @see [[org.scalatest.concurrent.Eventually.eventually]]
     */
    def waitFor(name: String, timeout: Duration = 150.millis)(predicate: Float => Boolean): Unit =
      this.assertGauge(name, predicate, timeout)

    private def assertGauge(
      name: String,
      predicate: Float => Boolean,
      timeout: Duration
    ): Assertion = eventually(timeout) {
      val actualValue: Float = this.apply(name, verbose = false)
      withClue(s"""Asserted predicate for gauge "$name" never evaluated to true:""") {
        predicate(actualValue) should be(true)
      }
    }

    private def assertGauge(
      name: String,
      expectedValue: Float,
      timeout: Duration = 150.millis
    ): Assertion = eventually(timeout) {
      val actualValue: Float = this.apply(name, verbose = false)
      withClue(s"""Expected "$expectedValue" for gauge "$name" but got "$actualValue":""") {
        actualValue should equal(expectedValue)
      }
    }

    private[InMemoryStatsReceiverUtility] def clear(): Unit =
      inMemoryStatsReceiver.gauges.clear()
  }

  /* Public */

  val counters: Counters = new Counters
  val stats: Stats = new Stats
  val gauges: Gauges = new Gauges

  /**
   * Clears all metrics of the underlying [[InMemoryStatsReceiver]].
   */
  def clear(): Unit = {
    counters.clear()
    stats.clear()
    gauges.clear()
  }

  /**
   * A complete sorted `Set` of all metric names collected by the underlying [[InMemoryStatsReceiver]].
   * The set is sorted by the natural ordering of the `String` metric name.
   *
   * @return a [[SortedSet]] of all the metric names collected by the underlying [[InMemoryStatsReceiver]].
   */
  def names: SortedSet[String] =
    stats.names ++ counters.names ++ gauges.names

  /**
   * Prints to `Stdout` all of the current metrics collected by the underlying
   * [[InMemoryStatsReceiver]].
   *
   * @see [[com.twitter.finagle.stats.InMemoryStatsReceiver.print]]
   */
  def print(): Unit = {
    val baos = new ByteArrayOutputStream(512)
    val ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())
    try {
      this.print(ps)
      val message = baos.toString(StandardCharsets.UTF_8.name())
      info(message)
    } finally {
      ps.close() // also closes the underlying ByteArrayOutputStream
    }
  }

  /**
   * Prints all of the current metrics collected by the underlying [[InMemoryStatsReceiver]]
   * to the given [[PrintStream]].
   *
   * @param p the [[PrintStream]] to use for output.
   *
   * @see [[com.twitter.finagle.stats.InMemoryStatsReceiver.print]]
   */
  def print(p: PrintStream): Unit =
    inMemoryStatsReceiver.print(p, includeHeaders = true)

  /** A version of [[eventually]] which accepts a [[com.twitter.util.Duration]] timeout. */
  private[this] def eventually[T](timeout: Duration)(fn: => T): T =
    eventually(timeout = PatienceConfiguration.Timeout(Span(timeout.inMillis, Millis)))(fn)

  /** Coverts `Seq("foo", "bar", "baz")` to `foo/bar/baz` */
  private[this] def keyStr(keys: Seq[String]): String = {
    keys.mkString("/")
  }
}
