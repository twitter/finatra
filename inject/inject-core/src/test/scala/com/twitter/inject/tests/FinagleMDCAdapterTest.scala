package com.twitter.inject.tests

import com.twitter.inject.Test
import com.twitter.inject.logging.FinagleMDCAdapter

class FinagleMDCAdapterTest extends Test {

  val adapter = new FinagleMDCAdapter()

  test("finagle MDC adapter") {
    adapter.put("name", "bob")
    adapter.get("name") should equal("bob")

    intercept[IllegalArgumentException] {
      adapter.put(null, "bar")
    }

    adapter.put("age", "10")
    adapter.get("age") should equal("10")

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
