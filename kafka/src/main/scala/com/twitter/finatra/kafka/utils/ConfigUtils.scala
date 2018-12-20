package com.twitter.finatra.kafka.utils

import java.util

object ConfigUtils {
  def getConfigOrElse(configs: util.Map[String, _], key: String, default: String): String = {
    Option(configs.get(key))
      .map(_.toString)
      .getOrElse(default)
  }
}
