package com.twitter.finatra.http.tests.request

import com.twitter.finagle.http.MapParamMap
import com.twitter.finatra.http.internal.request.RouteParamMap
import com.twitter.inject.Test

class RouteParamMapTest extends Test {

  val emptyFinagleParamMap = new MapParamMap(Map())

  "string params" in {
    val params = new RouteParamMap(
      emptyFinagleParamMap,
      Map("name" -> "bob"))

    params.get("name") should equal(Some("bob"))
    params.get("foo") should equal(None)
    params("name") should equal("bob")
    intercept[NoSuchElementException] {
      params("foo")
    }
  }

  "long params" in {
    val params = new RouteParamMap(
      emptyFinagleParamMap,
      Map("age" -> "10"))

    params.getLong("age") should equal(Some(10L))
    params.getLong("foo") should equal(None)
    params.getLong("age").get should equal(10L)
  }

  "int params" in {
    val params = new RouteParamMap(
      emptyFinagleParamMap,
      Map("age" -> "10"))
    params.getInt("age") should equal(Some(10))
  }

  "boolean params" in {
    val params = new RouteParamMap(
      emptyFinagleParamMap,
      Map(
        "male" -> "true",
        "female" -> "false"))

    params.getBoolean("male") should equal(Some(true))
    params.getBoolean("female") should equal(Some(false))
  }
}

