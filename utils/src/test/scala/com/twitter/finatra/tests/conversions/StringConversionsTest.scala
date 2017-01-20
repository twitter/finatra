package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.string._
import com.twitter.inject.WordSpecTest

class StringConversionsTest extends WordSpecTest {

  "toOption when nonEmpty" in {
    "foo".toOption should be(Some("foo"))
  }

  "toOption when empty" in {
    "".toOption should be(None)
  }

  "ellipse" in {
    "foobar".ellipse(2) should be("fo...")
  }
}
