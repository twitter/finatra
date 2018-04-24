package com.twitter.inject.tests.logging

import com.twitter.inject.Test
import com.twitter.inject.logging.MDCInitializer
import java.util.{HashMap => JHashMap}
import org.slf4j.MDC

class MDCInitializerTest extends Test {

  MDCInitializer.init()
  private val defaultContext = new JHashMap[String, String]()
  defaultContext.put("user_id", "1234")

  test("Test MDCInitializer") {
    MDC.get("name") should equal(null) // doesn't exist -- no map in the LocalContext, doesn't error
    MDC.put("name", "bob")  // does nothing -- no map in LocalContext
    MDC.get("name") should equal(null) // still doesn't exist


    MDCInitializer.let {
      MDC.get("user_id") should equal(null) // doesn't exist
      MDC.get("weight") should equal(null) // doesn't exist
      MDC.get("name") should equal(null) // doesn't exist

      MDC.put("name", "bob")
      MDC.get("name") should equal("bob")

      MDCInitializer.let {
        MDC.get("name") should equal(null) // doesn't exist
        MDC.put("weight", "200")

        MDC.get("weight") should equal("200")
      }

      MDC.get("weight") should equal(null) // doesn't exist

      intercept[IllegalArgumentException] {
        MDC.put(null, "bar")
      }

      MDC.put("age", "10")
      MDC.get("age") should equal("10")

      MDCInitializer.let(defaultContext) {
        MDC.get("user_id") should equal("1234")

        MDC.put("size", "small")
        MDC.get("size") should equal("small")
      }

      MDC.get("user_id") should equal(null) // doesn't exist
      MDC.get("size") should equal(null) // doesn't exist

      val copy = MDC.getCopyOfContextMap

      MDC.remove("name")
      MDC.get("name") should equal(null)
      MDC.get("age") should equal("10")

      MDC.clear()
      MDC.get("name") should equal(null)
      MDC.get("age") should equal(null)

      MDC.setContextMap(copy)
      MDC.get("name") should equal("bob")
      MDC.get("age") should equal("10")
    }
  }

}
