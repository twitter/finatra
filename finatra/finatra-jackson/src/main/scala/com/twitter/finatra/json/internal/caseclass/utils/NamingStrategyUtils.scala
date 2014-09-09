package com.twitter.finatra.json.internal.caseclass.utils

import com.fasterxml.jackson.databind.PropertyNamingStrategy

object NamingStrategyUtils {
  val CamelCasePropertyNamingStrategy = new PropertyNamingStrategy {}
}
