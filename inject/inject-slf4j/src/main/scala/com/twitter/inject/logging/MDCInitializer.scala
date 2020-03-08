package com.twitter.inject.logging

import com.twitter.concurrent.Once
import com.twitter.finagle.context.Contexts
import com.twitter.inject.Logging
import java.util.{HashMap => JHashMap}
import org.slf4j.FinagleMDCInitializer

/**
 * Local contexts helpers for the framework SLF4J MDC integration [[FinagleMDCAdapter]].
 * The backing of the adapter is stored as a [[com.twitter.finagle.context.LocalContext]].
 *
 * Local contexts have lifetimes bound by Finagle server requests. They are local to
 * the process, that is they are not propagated to other services.
 *
 * @see [[https://logback.qos.ch/manual/mdc.html Mapped Diagnostic Context]]
 * @see [[com.twitter.inject.logging.FinagleMDCAdapter]]
 * @see [[com.twitter.finagle.context.LocalContext]]
 */
object MDCInitializer extends Logging {

  /** LocalContext Key */
  private[logging] val Key = new Contexts.local.Key[JHashMap[String, String]]()

  /** Ensures initialization of the FinagleMDCInitializer occurs only once */
  private val initialize = Once {
    FinagleMDCInitializer.init()
  }

  /**
   * Initialize the MDC integration. This methods ensures the default MDC initialization runs and
   * swaps out the default MDCAdapter for the framework Finagle-aware implementation:
   * [[com.twitter.inject.logging.FinagleMDCAdapter]]
   *
   * @see [[org.slf4j.FinagleMDCInitializer.init()]]
   */
  def init(): Unit = {
    info("Initialized MDC.")
    initialize()
  }

  /**
   * Initializes the value of the MDC to a new empty [[java.util.HashMap]] only for the scope of the
   * current [[com.twitter.finagle.context.LocalContext]] for the given function `fn`.
   *
   * @param fn the function to execute with the given LocalContext.
   * @return the result of executing the function `fn`.
   */
  def let[R](fn: => R): R = let(new JHashMap[String, String]())(fn)

  /**
   * Initializes the value of the MDC to the given [[java.util.HashMap]] only for the scope of the
   * current [[com.twitter.finagle.context.LocalContext]] for the given function `fn`.
   *
   * @param map the [[java.util.HashMap]] to set as the initial MDC state.
   * @param fn the function to execute with the given LocalContext.
   * @return the result of executing the function `fn`.
   */
  def let[R](map: JHashMap[String, String])(fn: => R): R = {
    Contexts.local.let(Key, map) {
      fn
    }
  }

  /** Returns the current value of the MDC [[java.util.HashMap]] state. */
  def current: Option[JHashMap[String, String]] = Contexts.local.get(Key)
}
