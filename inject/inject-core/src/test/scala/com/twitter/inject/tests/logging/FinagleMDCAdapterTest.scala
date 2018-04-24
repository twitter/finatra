package com.twitter.inject.tests.logging

import com.twitter.inject.Test
import com.twitter.inject.logging.{FinagleMDCAdapter, MDCInitializer}
import java.util.{HashMap => JHashMap}

class FinagleMDCAdapterTest extends Test {

  val adapter = new FinagleMDCAdapter()
  private val defaultContext = new JHashMap[String, String]()
  defaultContext.put("user_id", "1234")

  test("finagle MDC adapter") {
    MDCInitializer.let {
      adapter.get("user_id") should equal(null) // doesn't exist
      adapter.get("weight") should equal(null) // doesn't exist
      adapter.get("name") should equal(null) // doesn't exist

      adapter.put("name", "bob")
      adapter.get("name") should equal("bob")

      MDCInitializer.let {
        adapter.get("name") should equal(null) // doesn't exist
        adapter.put("weight", "200")

        adapter.get("weight") should equal("200")
      }

      adapter.get("weight") should equal(null) // doesn't exist

      intercept[IllegalArgumentException] {
        adapter.put(null, "bar")
      }

      adapter.put("age", "10")
      adapter.get("age") should equal("10")

      MDCInitializer.let(defaultContext) {
        adapter.get("user_id") should equal("1234")

        adapter.put("size", "small")
        adapter.get("size") should equal("small")
      }

      adapter.get("user_id") should equal(null) // doesn't exist
      adapter.get("size") should equal(null) // doesn't exist

      val copy = adapter.getCopyOfContextMap

      adapter.remove("name")
      adapter.get("name") should equal(null)
      adapter.get("age") should equal("10")

      adapter.clear()
      adapter.get("name") should equal(null)
      adapter.get("age") should equal(null)

      adapter.setContextMap(copy)
      adapter.get("name") should equal("bob")
      adapter.get("age") should equal("10")
    }
  }

  test("finagle MDC adapter -- no let") {
    adapter.get("name") should equal(null) // doesn't exist -- no map in the LocalContext, doesn't error
    adapter.put("name", "bob")  // does nothing -- no map in LocalContext
    adapter.get("name") should equal(null) // still doesn't exist
  }
}
