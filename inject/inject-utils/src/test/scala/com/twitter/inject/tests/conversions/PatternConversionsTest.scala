package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.pattern._

class PatternConversionsTest extends Test {

  val testRegex = ".*abc.*".r

  test("RichPattern#matches") {
    testRegex.matches("123abc456") should be(true)
  }
  test("RichPattern#doesnt match") {
    testRegex.matches("123") should be(false)
  }
}
