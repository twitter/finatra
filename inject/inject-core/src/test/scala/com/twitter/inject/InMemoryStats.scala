package com.twitter.inject

import com.twitter.util.Duration
import scala.collection.{SortedMap, SortedSet}

trait InMemoryStats[T] {

  /**
   * Returns a sorted `Map` of collected metric values from the
   * underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]]. The map is sorted by the natural ordering of the
   * `String` key which represents the metric name.
   *
   * @return a [[SortedMap]] of metric name to T representing
   *         the current metric value for the given name.
   */
  def toSortedMap: SortedMap[String, T]

  /**
   * A complete sorted `Set` of all metric names collected by
   * the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]]. The set is sorted by the natural ordering of the
   * `String` metric name.
   *
   * @return a [[SortedSet]] of all the metric names collected
   *         by the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]].
   */
  def names: SortedSet[String]

  /**
   * Returns an `Option[T]` value for the metric collected with
   * the given name. If a metric exists with the given name, then
   * `Some(value)` will be returned, otherwise a [[None]].
   *
   * @param name the registered `String` to use as the lookup key for a collected
   *             metric.
   *
   * @return the current value of the metric wrapped in a [[Some]],
   *         if it exists in the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]], otherwise [[None]].
   */
  def get(name: String): Option[T]

  /**
   * Returns the `T` value for the metric collected with
   * the given name or an [[IllegalArgumentException]] if the metric
   * does not exist in the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]].
   *
   * @param name the registered `String` to use as the lookup key for a collected
   *             metric.
   *
   * @return the current `T` value of the metric if it exists
   *         in the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]], otherwise an [[IllegalArgumentException]].
   */
  def apply(name: String): T

  /**
   * Asserts that the metric represented by the given name
   * has the given expected value. If the returned value does match the expected a
   * [[org.scalatest.exceptions.TestFailedException]] will be thrown.
   *
   * @note If a metric with the given name does not exist an
   * [[IllegalArgumentException]] will be thrown.
   *
   * @param name the identifier of the metric to lookup
   *             from the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]].
   * @param expected the expected value of the metric.
   */
  def assert(name: String, expected: T): Unit

  /** For Java compatibility */
  def expect(name: String, expected: T): Unit

  /**
   * Asserts that the passed predicate callback function executed on a metric
   * represented by the given name returns `True`. If the executed callback function does not
   * return `true` a [[org.scalatest.exceptions.TestFailedException]] will be thrown.
   *
   * @note If a metric with the given name does not exist an
   *       [[IllegalArgumentException]] will be thrown.
   *
   * @param name the identifier of the metric to lookup
   *             from the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]].
   * @param predicate the function to execute on the looked up metric
   *                 which SHOULD return `True`.
   */
  def assert(name: String)(predicate: T => Boolean): Unit

  /**
   * Waits for the metric represented by the given name to
   * equal the given expected value. If a metric value is not found
   * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
   * will be thrown.
   *
   * @note The default timeout is 150ms. To specify a different timeout, users should use
   *       [[waitFor(name: String, timeout: Duration)(predicate: T => Boolean)]].
   *
   * @param name the identifier of the metric to lookup
   *             from the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]].
   * @param expected the expected value of the metric.
   *
   * @see [[org.scalatest.concurrent.Eventually.eventually]]
   * @see [[waitFor(name: String, predicate: T => Boolean, timeout: Duration)]]
   */
  def waitFor(name: String, expected: T): Unit

  /**
   * Waits for the metric represented by the given name to
   * equal the given expected value. If a metric value is not found
   * or does not match the expected a [[org.scalatest.exceptions.TestFailedDueToTimeoutException]]
   * will be thrown.
   *
   * @param name      the identifier of the metric to lookup
   *                  from the underlying [[com.twitter.finagle.stats.InMemoryStatsReceiver]].
   * @param timeout   the [[com.twitter.util.Duration]] to wait for the retrieval of the counter and
   *                  the predicate to return `True`. The default is 150 ms.
   * @param predicate the function to execute on the retrieved metric
   *                  which is expected to return `True`.
   *
   * @see [[org.scalatest.concurrent.Eventually.eventually]]
   */
  def waitFor(name: String, timeout: Duration)(predicate: T => Boolean): Unit

}
