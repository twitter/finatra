package org.slf4j

import com.twitter.inject.logging.FinagleMDCAdapter
import java.lang.reflect.Field

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

    // SLF4J 2.x made MDC_ADAPTER private, use reflection to swap in the Finagle adapter
    try {
      val mdcClass = classOf[MDC]
      val field = mdcClass.getDeclaredField("MDC_ADAPTER")
      field.setAccessible(true)

      // Set the Finagle MDC adapter
      field.set(null, new FinagleMDCAdapter)
    } catch {
      case e: Exception =>
        throw new RuntimeException(
          "Failed to initialize FinagleMDCAdapter. " +
          "This may be due to JVM security restrictions or SLF4J API changes.", e)
    }
  }
}
