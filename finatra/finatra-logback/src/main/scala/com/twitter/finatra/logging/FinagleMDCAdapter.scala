package com.twitter.finatra.logging

import com.twitter.finatra.logging.FinagleMDCAdapter._
import com.twitter.util.Local
import java.util.{HashMap => JavaHashMap, Map => JavaMap}
import org.slf4j.spi.MDCAdapter

object FinagleMDCAdapter {
  private val LocalMap = new Local[JavaMap[String, String]]
}

final class FinagleMDCAdapter extends MDCAdapter {

  override def put(key: String, value: String) {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null")
    }

    val map = getOrCreateMap()
    map.put(key, value)
  }

  override def get(key: String): String = {
    (for (map <- LocalMap()) yield {
      map.get(key)
    }).getOrElse(null)
  }

  override def remove(key: String) {
    for (map <- LocalMap()) {
      map.remove(key)
    }
  }

  override def clear() {
    LocalMap.clear()
  }

  override def getCopyOfContextMap: JavaMap[String, String] = {
    (for (map <- LocalMap()) yield {
      new JavaHashMap[String, String](map)
    }).orNull
  }

  override def setContextMap(contextMap: JavaMap[String, String]) {
    val newMap = new JavaHashMap[String, String](contextMap)
    LocalMap.update(newMap)
  }

  private def getOrCreateMap(): JavaMap[String, String] = {
    LocalMap().getOrElse {
      val newMap = new JavaHashMap[String, String]()
      LocalMap.update(newMap)
      newMap
    }
  }
}
