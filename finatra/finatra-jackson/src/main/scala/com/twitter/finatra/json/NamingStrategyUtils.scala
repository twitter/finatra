package com.twitter.finatra.json

import com.fasterxml.jackson.databind.PropertyNamingStrategy

object NamingStrategyUtils {
  val CamelCasePropertyNamingStrategy = new PropertyNamingStrategy {}
}
