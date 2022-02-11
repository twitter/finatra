package com.twitter.inject.logging

import com.twitter.finagle.context.Contexts
import java.util.Collections
import java.util.{HashMap => JHashMap}
import java.util.{Map => JMap}
import org.slf4j.spi.MDCAdapter

/**
 * Implementation of the [[org.slf4j.spi.MDCAdapter]] which stores the backing map as a
 * [[com.twitter.finagle.context.LocalContext]] via a [[com.twitter.finagle.context.Contexts.local]].
 *
 * @note Users are not expected to interact with this MDCAdapter directly. Users should use the
 *       framework MDC API, via [[com.twitter.inject.logging.MDCInitializer]].
 * @note if access to the org.slf4j.MDC is done without first calling [[com.twitter.inject.logging.MDCInitializer.let]]
 *       these operations are all effectively no-ops.
 * @see [[com.twitter.inject.logging.MDCInitializer]]
 * @see [[com.twitter.finagle.context.Contexts.local]]
 * @see [[com.twitter.finagle.context.LocalContext]]
 */
final class FinagleMDCAdapter extends MDCAdapter {

  /**
   * Put a context value (the val parameter) as identified with the key parameter into the current
   * thread's context map. The key parameter cannot be null. The val parameter can be null only if
   * the underlying implementation supports it.
   *
   * If the current thread does not have a context map it is created as a side effect of this call.
   *
   * @see [[org.slf4j.spi.MDCAdapter#put]]
   */
  def put(key: String, value: String): Unit = {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null")
    }

    for (map <- Contexts.local.get(MDCInitializer.Key)) {
      map.put(key, value)
    }
  }

  /**
   * Get the context identified by the key parameter. The key parameter cannot be null.
   *
   * @return the string value identified by the key parameter.
   * @see [[org.slf4j.spi.MDCAdapter#get]]
   */
  def get(key: String): String = {
    (for (map <- Contexts.local.get(MDCInitializer.Key)) yield {
      map.get(key)
    }).orNull
  }

  /**
   * Remove the the context identified by the key parameter. The key parameter cannot be null.
   * This method does nothing if there is no previous value associated with key.
   *
   * @see [[org.slf4j.spi.MDCAdapter#remove]]
   */
  def remove(key: String): Unit = {
    for (map <- Contexts.local.get(MDCInitializer.Key)) {
      map.remove(key)
    }
  }

  /**
   * Clear all entries in the MDC.
   *
   * @see [[org.slf4j.spi.MDCAdapter#clear]]
   */
  def clear(): Unit = {
    for (map <- Contexts.local.get(MDCInitializer.Key)) {
      map.clear()
    }
  }

  /**
   * Return a copy of the current thread's context map, with keys and values of type String.
   * Returned value may be null.
   *
   * @return A copy of the current thread's context map. May be null.
   * @see [[org.slf4j.spi.MDCAdapter#getCopyOfContextMap]]
   */
  def getCopyOfContextMap: JMap[String, String] = {
    (for (map <- Contexts.local.get(MDCInitializer.Key)) yield {
      new JHashMap[String, String](map)
    }).orNull
  }

  /**
   * Set the current thread's context map by first clearing any existing map and then copying the
   * map passed as parameter. The context map parameter must only contain keys and values of type
   * String.
   *
   * @param contextMap must contain only keys and values of type String
   * @see [[org.slf4j.spi.MDCAdapter#setContextMap]]
   */
  def setContextMap(contextMap: JMap[String, String]): Unit = {
    for (map <- Contexts.local.get(MDCInitializer.Key)) yield {
      map.clear()
      map.putAll(contextMap)
    }
  }

  /* Private */

  /** FOR INTERNAL USE ONLY */
  private[twitter] def getPropertyContextMap: JMap[String, String] = {
    Contexts.local.get(MDCInitializer.Key) match {
      case Some(map) => map
      case _ => Collections.emptyMap()
    }
  }
}
