package com.twitter.finatra.annotations

import com.twitter.finatra.test.Test

class FlagImplTest extends Test {

  val flag1 = new FlagImpl("a")
  val flag2 = new FlagImpl("a")
  val flag3 = new FlagImpl("b")

  "equals" in {
    flag1 should equal(flag2)
    flag1 should not equal(flag3)
  }

  "hashcode" in {
    pending
    flag1.hashCode() should equal(flag2.hashCode())
    flag1.hashCode() should not equal(flag3.hashCode())
  }
}
