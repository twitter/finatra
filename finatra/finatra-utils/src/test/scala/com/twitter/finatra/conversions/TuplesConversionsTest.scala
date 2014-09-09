package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.tuples._
import com.twitter.finatra.test.Test


class TuplesConversionsTest extends Test {

  val tuples = Seq(1 -> "bob", 2 -> "sally")

  "RichTuple" should {
    "#toKeys" in {
      tuples.toKeys should equal(Seq(1, 2))
    }
    "#toKeySet" in {
      tuples.toKeySet should equal(Set(1, 2))
    }
    "#toValues" in {
      tuples.toValues should equal(Seq("bob", "sally"))
    }
    "#mapValues" in {
      tuples.mapValues {_.size} should equal(Seq(1 -> 3, 2 -> 5))
    }
    "#toMultiMap" in {
      val multiTuples = Seq(1 -> "a", 1 -> "b", 2 -> "ab")
      multiTuples.toMultiMap should equal(Map(1 -> Seq("b", "a"), 2-> Seq("ab")))
    }
  }
}
