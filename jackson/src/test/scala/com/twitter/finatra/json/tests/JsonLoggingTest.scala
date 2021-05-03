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

  // Set SLFJ Simple Logger log level to Info
  System.setProperty("org.slf4j.simpleLogger.log.com.twitter.finatra.json.tests", "info")

  test("log params should be evaluated for log levels >= the configured info level") {
    var msgCalled = 0
    def msg: String = {
      msgCalled += 1
      "foo"
    }

    var fooBarCalled = 0
    def fooBar: FooBar = {
      fooBarCalled += 1
      FooBar("steve")
    }

    infoJson(msg, fooBar)
    infoPretty(msg, fooBar)

    warnJson(msg, fooBar)
    warnPretty(msg, fooBar)

    errorJson(msg, fooBar)
    errorPretty(msg, fooBar)

    msgCalled should be(6)
    fooBarCalled should be(6)
  }

  test("log params should *not* be evaluated for log levels < the configured info level") {
    def msg: String = {
      fail("msg param should not be executed based on the configured log level")
      "foo"
    }
    def fooBar: FooBar = {
      fail("msg param should not be executed based on the configured log level")
      FooBar("steve")
    }

    debugJson(msg, fooBar)
    debugPretty(msg, fooBar)

    traceJson(msg, fooBar)
    tracePretty(msg, fooBar)
  }
}
