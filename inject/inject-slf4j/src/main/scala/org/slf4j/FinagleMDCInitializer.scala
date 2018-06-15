package org.slf4j

import com.twitter.inject.logging.FinagleMDCAdapter

/**
 * Sets up the [[com.twitter.inject.logging.FinagleMDCAdapter]] as the [[org.slf4j.spi.MDCAdapter]]
 * implementation which provides MDC integration through a Finagle request via a
 * [[com.twitter.util.Local]].
 *
 * @note Users should not need to interact with this adapter directly. Initialization
 *       of the MDC integration can be done through [[com.twitter.inject.logging.MDCInitializer.init()]]
 * @see [[org.slf4j.MDC]]
 * @see [[com.twitter.inject.logging.MDCInitializer.init()]]
 * @see [[com.twitter.inject.logging.FinagleMDCAdapter]]
 * @see [[com.twitter.util.Local]]
 */
object FinagleMDCInitializer {

  /**
   * @note Prefer using [[com.twitter.inject.logging.MDCInitializer.init()]]
   * @see [[com.twitter.inject.logging.MDCInitializer.init()]]
   */
  def init(): Unit = {
    MDC.getMDCAdapter // Make sure default MDC static initializer has run
    MDC.mdcAdapter = new FinagleMDCAdapter // Swap in the Finagle adapter
  }
}
