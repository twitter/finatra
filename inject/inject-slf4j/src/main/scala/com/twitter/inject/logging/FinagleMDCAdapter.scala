package com.twitter.inject.logging

import com.google.common.collect.ImmutableMap
import com.twitter.util.Local
import java.util.{HashMap => JHashMap, Map => JMap}
import org.slf4j.spi.MDCAdapter

final class FinagleMDCAdapter extends MDCAdapter {

  private[this] val local = new Local[JMap[String, String]]

  override def put(key: String, value: String) {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null")
    }

    val map = getOrCreateMap()
    map.put(key, value)
  }

  override def get(key: String): String = {
    (for (map <- local()) yield {
      map.get(key)
    }).orNull
  }

  override def remove(key: String) {
    for (map <- local()) {
      map.remove(key)
    }
  }

  override def clear() {
    local.clear()
  }

  override def getCopyOfContextMap: JMap[String, String] = {
    (for (map <- local()) yield {
      new JHashMap[String, String](map)
    }).orNull
  }

  override def setContextMap(contextMap: JMap[String, String]) {
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
      case _ => ImmutableMap.of()
    }
  }
}
