package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.string._

class StringConversionsTest extends Test {

  test("ellipse") {
    "foobar".ellipse(2) should be("fo...")
  }
}
