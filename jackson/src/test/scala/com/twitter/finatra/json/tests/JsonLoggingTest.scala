package com.twitter.finatra.json.tests

import com.twitter.finatra.json.JsonLogging
import com.twitter.inject.WordSpecTest

class JsonLoggingTest
  extends WordSpecTest
  with JsonLogging {

  "json logging" in {
    val fooBar = FooBar("steve")
    infoJson("foo", fooBar)
    infoPretty("foo", fooBar)

    debugJson("foo", fooBar)
    debugPretty("foo", fooBar)

    traceJson("foo", fooBar)
    tracePretty("foo", fooBar)

    warnJson("foo", fooBar)
    warnPretty("foo", fooBar)

    errorJson("foo", fooBar)
    errorPretty("foo", fooBar)
  }
}

case class FooBar(
  name: String)