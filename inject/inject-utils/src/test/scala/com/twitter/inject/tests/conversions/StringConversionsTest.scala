package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.string._

class StringConversionsTest extends Test {

  test("toOption#when nonEmpty") {
    "foo".toOption should be(Some("foo"))
  }

  test("toOption#when empty") {
    "".toOption should be(None)
  }

  test("ellipse") {
    "foobar".ellipse(2) should be("fo...")
  }

  test("camelify") {
    "foo_bar".camelify should be("fooBar")
  }

  test("pascalify") {
    "foo_bar".pascalify should be("FooBar")
  }

  test("snakify") {
    "FooBar".snakify should be("foo_bar")
  }
}
