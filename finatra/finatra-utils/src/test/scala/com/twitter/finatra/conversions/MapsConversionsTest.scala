package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.maps._
import com.twitter.finatra.test.Test

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
  }
}
