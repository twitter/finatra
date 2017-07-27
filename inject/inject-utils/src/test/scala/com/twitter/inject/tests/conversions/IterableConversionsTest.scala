package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.iterable._

class IterableConversionsTest extends Test {

  test("RichIterable#distinctBy") {
    Seq("a", "b", "aa", "aaa", "bb", "c") distinctBy { _.size } should equal(Seq("a", "aa", "aaa"))
  }
}
