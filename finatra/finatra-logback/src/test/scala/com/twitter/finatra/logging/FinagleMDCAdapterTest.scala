package com.twitter.finatra.logging

import com.twitter.inject.Test

class FinagleMDCAdapterTest extends Test {

  val adapter = new FinagleMDCAdapter()

  "test" in {
    adapter.put("name", "bob")
    adapter.get("name") should equal("bob")

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
