package com.twitter.inject.server.tests

import com.twitter.finagle.stats.{Counter, Gauge, InMemoryStatsReceiver, Stat, StatsReceiver, Verbosity}
import java.io.PrintStream
import scala.collection.mutable

class TestStatsReceiver extends StatsReceiver {
  private[this] val underlying: InMemoryStatsReceiver = new InMemoryStatsReceiver

  val counters: mutable.Map[Seq[String], Long] = underlying.counters

  val stats: mutable.Map[Seq[String], Seq[Float]] = underlying.stats

  val gauges: mutable.Map[Seq[String], () => Float] = underlying.gauges

  /**
   * Specifies the representative receiver.  This is in order to
   * expose an object we can use for comparison so that global stats
   * are only reported once per receiver.
   */
  override def repr: TestStatsReceiver = this

  /**
   * Get a [[Counter counter]] with the given `name`.
   */
  override def counter(verbosity: Verbosity, name: String*): Counter =
    underlying.counter(verbosity, name: _*)

  /**
   * Get a [[Stat stat]] with the given name.
   */
  override def stat(verbosity: Verbosity, name: String*): Stat =
    underlying.stat(verbosity, name: _*)

  /**
   * Add the function `f` as a [[Gauge gauge]] with the given name.
   *
   * The returned [[Gauge gauge]] value is only weakly referenced by the
   * [[StatsReceiver]], and if garbage collected will eventually cease to
   * be a part of this measurement: thus, it needs to be retained by the
   * caller. Or put another way, the measurement is only guaranteed to exist
   * as long as there exists a strong reference to the returned
   * [[Gauge gauge]] and typically should be stored in a member variable.
   *
   * Measurements under the same name are added together.
   *
   * @see [[StatsReceiver.provideGauge]] when there is not a good location
   *      to store the returned [[Gauge gauge]] that can give the desired lifecycle.
   * @see [[https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html java.lang.ref.WeakReference]]
   */
  override def addGauge(verbosity: Verbosity, name: String*)(f: => Float): Gauge =
    underlying.addGauge(verbosity, name: _*)(f)

  override def toString: String = "TestStatsReceiver"

  def print(p: PrintStream): Unit = print(p, includeHeaders = false)

  def print(p: PrintStream, includeHeaders: Boolean): Unit = underlying.print(p, includeHeaders)
}
