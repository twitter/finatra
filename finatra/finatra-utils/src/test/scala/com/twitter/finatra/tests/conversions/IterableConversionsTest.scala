package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.iterable._
import com.twitter.inject.Test


class IterableConversionsTest extends Test {

  "RichIterable" should {
    "#distinctBy" in {
      Seq("a", "b", "aa", "aaa", "bb", "c") distinctBy {_.size} should equal(Seq("a", "aa", "aaa"))
    }
  }
}
