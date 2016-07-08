package com.twitter.inject.tests.conversions

import com.twitter.inject.conversions.map._
import com.twitter.inject.Test
import scala.collection.SortedMap

class MapsConversionsTest extends Test {

  "RichSortedMap" should {
    "#mapKeys" in {
      SortedMap(1 -> "a") mapKeys { _.toString } should
        equal(Map("1" -> "a"))
    }
  }

  "RichSortedMapOfSortedMap" should {
    "swapKeys" in {
      SortedMap(1 -> SortedMap("a" -> "z")).swapKeys should equal(SortedMap("a" -> SortedMap(1 -> "z")))
    }
  }

  "Rich map of maps" should {
    "swapKeys" in {
      Map(1 -> Map("a" -> "z")).swapKeys should equal(Map("a" -> Map(1 -> "z")))
    }
  }

  "Map[K, Option[V]]" should {
    "#flattenEntries" in {
      Map("a" -> Some(1)).flattenEntries should equal(Map("a" -> 1))
      Map("a" -> None).flattenEntries should equal(Map())
      Map("a" -> None, "b" -> Some(2)).flattenEntries should equal(Map("b" -> 2))
    }
  }

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