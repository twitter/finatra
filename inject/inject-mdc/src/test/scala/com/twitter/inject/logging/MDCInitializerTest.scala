package com.twitter.inject.logging

import com.twitter.inject.Test
import java.util.{HashMap => JHashMap}
import org.slf4j.MDC

class MDCInitializerTest extends Test {

  MDCInitializer.init()
  MDCInitializer.current.isEmpty should be(true)

  private val defaultContext = new JHashMap[String, String]()
  defaultContext.put("user_id", "1234")

  test("Test MDCInitializer") {
    MDC.get("name") should equal(null) // doesn't exist -- no map in the LocalContext, doesn't error
    MDC.put("name", "bob") // does nothing -- no map in LocalContext
    MDC.get("name") should equal(null) // still doesn't exist

    MDCInitializer.let {
      MDCInitializer.current.isEmpty should be(false)

      MDC.get("user_id") should equal(null) // doesn't exist
      MDC.get("weight") should equal(null) // doesn't exist
      MDC.get("name") should equal(null) // doesn't exist

      MDC.put("name", "bob")
      MDC.get("name") should equal("bob")
      MDCInitializer.current.get.get("name") should equal("bob")
      MDCInitializer.current.get.size() should be(1)

      MDCInitializer.let {
        MDCInitializer.current.isEmpty should be(false)
        MDCInitializer.current.get.isEmpty should be(true) // local has started over in this closure

        MDC.get("name") should equal(null) // doesn't exist
        MDC.put("weight", "200")

        MDC.get("weight") should equal("200")
        MDCInitializer.current.get.get("weight") should equal("200")
        MDCInitializer.current.get.size() should be(1)
      }

      MDC.get("weight") should equal(null) // doesn't exist

      intercept[IllegalArgumentException] {
        MDC.put(null, "bar")
      }

      MDC.put("age", "10")
      MDC.get("age") should equal("10")

      MDCInitializer.let(defaultContext) {
        MDC.get("user_id") should equal("1234")
        MDCInitializer.current.get.get("user_id") should equal("1234")

        MDC.put("size", "small")
        MDC.get("size") should equal("small")
        MDCInitializer.current.get.get("size") should equal("small")
        MDCInitializer.current.get.size() should be(2)
      }
      MDCInitializer.current.get.size() should be(
        2
      ) // back to the previous state which has two entries now
      MDCInitializer.current.get.get("name") should equal("bob")
      MDCInitializer.current.get.get("age") should equal("10")

      MDC.get("user_id") should equal(null) // doesn't exist
      MDC.get("size") should equal(null) // doesn't exist

      val copy = MDC.getCopyOfContextMap

      MDC.remove("name")
      MDC.get("name") should equal(null)
      MDC.get("age") should equal("10")

      MDC.clear()
      MDC.get("name") should equal(null)
      MDC.get("age") should equal(null)
      MDCInitializer.current.isDefined should be(true)
      MDCInitializer.current.get.isEmpty should be(true)

      MDC.setContextMap(copy)
      MDC.get("name") should equal("bob")
      MDC.get("age") should equal("10")
      MDCInitializer.current.isDefined should be(true)
      MDCInitializer.current.get.isEmpty should be(false)
      MDCInitializer.current.isDefined should be(true)
      MDCInitializer.current.get.get("name") should equal("bob")
      MDCInitializer.current.get.get("age") should equal("10")
    }
  }

}
