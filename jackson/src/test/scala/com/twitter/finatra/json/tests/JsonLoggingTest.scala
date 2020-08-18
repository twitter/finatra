package com.twitter.finatra.json.tests

import com.fasterxml.jackson.annotation.JsonProperty
import com.twitter.finatra.jackson.TestInjectableValue
import com.twitter.finatra.json.JsonLogging
import com.twitter.inject.Test

object JsonLoggingTest {
  trait Bar {
    @JsonProperty("helloWorld") @TestInjectableValue(value = "accept")
    def hello: String
  }
  case class FooBar(hello: String) extends Bar
}

class JsonLoggingTest extends Test with JsonLogging {
  import JsonLoggingTest._

  test("json logging") {
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
