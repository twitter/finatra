package com.twitter.finatra.logging

import com.twitter.util.Local
import java.util.{HashMap => JavaHashMap, Map => JavaMap}
import org.slf4j.spi.MDCAdapter

final class FinagleMDCAdapter extends MDCAdapter {

  private[this] val local = new Local[JavaMap[String, String]]

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

  override def getCopyOfContextMap: JavaMap[String, String] = {
    (for (map <- local()) yield {
      new JavaHashMap[String, String](map)
    }).orNull
  }

  override def setContextMap(contextMap: JavaMap[String, String]) {
    val copiedMap = new JavaHashMap[String, String](contextMap)
    local.update(copiedMap)
  }

  private def getOrCreateMap(): JavaMap[String, String] = {
    local().getOrElse {
      val newMap = new JavaHashMap[String, String]()
      local.update(newMap)
      newMap
    }
  }
}
