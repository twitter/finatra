package com.twitter.finatra.request

import com.twitter.finatra.test.Test

class RouteParamsTest extends Test {

  "string params" in {
    val params = RouteParams(Map("name" -> "bob"))

    params.get("name") should equal(Some("bob"))
    params.get("foo") should equal(None)
    params("name") should equal("bob")
    intercept[NoSuchElementException] {
      params("foo")
    }
  }

  "long params" in {
    val params = RouteParams(Map("age" -> "10"))

    params.getLong("age") should equal(Some(10L))
    params.getLong("foo") should equal(None)
    params.long("age") should equal(10L)
    intercept[NoSuchElementException] {
      params.long("foo")
    }
  }

  "int params" in {
    val params = RouteParams(Map("age" -> "10"))
    params.getInt("age") should equal(Some(10))
  }

  "boolean params" in {
    val params = RouteParams(Map(
      "male" -> "true",
      "female" -> "false"))

    params.getBoolean("male") should equal(Some(true))
    params.getBoolean("female") should equal(Some(false))
  }
}

