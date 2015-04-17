package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.pattern._
import com.twitter.inject.Test

class PatternConversionsTest extends Test {

  "RichPattern" should {
    val regex = ".*abc.*".r

    "matches" in {
      regex.matches("123abc456") should be(true)
    }
    "doesnt match" in {
      regex.matches("123") should be(false)
    }
  }
}
