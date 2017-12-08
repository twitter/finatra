package com.twitter.inject.logging

import com.twitter.util.Local
import java.util.{Collections, HashMap => JHashMap, Map => JMap}
import org.slf4j.spi.MDCAdapter

final class FinagleMDCAdapter extends MDCAdapter {

  private[this] val local = new Local[JMap[String, String]]

  def put(key: String, value: String): Unit = {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null")
    }

    val map = getOrCreateMap()
    map.put(key, value)
  }

  def get(key: String): String = {
    (for (map <- local()) yield {
      map.get(key)
    }).orNull
  }

  def remove(key: String): Unit = {
    for (map <- local()) {
      map.remove(key)
    }
  }

  def clear(): Unit = {
    local.clear()
  }

  def getCopyOfContextMap: JMap[String, String] = {
    (for (map <- local()) yield {
      new JHashMap[String, String](map)
    }).orNull
  }

  def setContextMap(contextMap: JMap[String, String]): Unit = {
    val copiedMap = new JHashMap[String, String](contextMap)
    local.update(copiedMap)
  }

  /* Private */

  private def getOrCreateMap(): JMap[String, String] = {
    local() match {
      case Some(map) => map
      case _ =>
        val newMap = new JHashMap[String, String]()
        local.update(newMap)
        newMap
    }
  }

  /** FOR INTERNAL USE ONLY */
  private[twitter] def getPropertyContextMap: JMap[String, String] = {
    local() match {
      case Some(map) => map
      case _ => Collections.emptyMap()
    }
  }
}
