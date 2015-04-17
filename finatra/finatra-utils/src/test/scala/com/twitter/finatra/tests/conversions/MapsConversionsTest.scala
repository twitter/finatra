package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.map._
import com.twitter.inject.Test

class MapsConversionsTest extends Test {

  "RichMap" should {
    "#mapKeys" in {
      Map(1 -> "a") mapKeys {_.toString} should
        equal(Map("1" -> "a"))
    }

    "#invert simple" in {
      Map(1 -> "a").invert should
        equal(Map("a" -> Iterable(1)))
    }

    "#invert complex" in {
      Map(1 -> "a", 2 -> "a", 3 -> "b").invert should
        equal(Map("a" -> Iterable(1, 2), "b" -> Iterable(3)))
    }

    "#invertSingleValue" in {
      Map(1 -> "a", 2 -> "a", 3 -> "b").invertSingleValue should
        equal(Map("a" -> 2, "b" -> 3))
    }

    "#filterValues" in {
      Map(1 -> "a", 2 -> "a", 3 -> "b") filterValues {_ == "b"} should
        equal(Map(3 -> "b"))
    }

    "#filterNotValues" in {
      Map(1 -> "a", 2 -> "a", 3 -> "b") filterNotValues {_ == "b"} should
        equal(Map(1 -> "a", 2 -> "a"))
    }

    "#filterNotKeys" in {
      Map(1 -> "a", 2 -> "a", 3 -> "b") filterNotKeys {_ == 3} should
        equal(Map(1 -> "a", 2 -> "a"))
    }
  }
}
