package com.twitter.finatra.tests

import org.joda.time.DateTimeZone
import org.scalatest.{Matchers, WordSpec}

abstract class JsonTest extends WordSpec with Matchers {
  DateTimeZone.setDefault(DateTimeZone.UTC)
}
