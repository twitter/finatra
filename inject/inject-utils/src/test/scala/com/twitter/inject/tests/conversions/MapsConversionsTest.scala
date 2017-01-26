package com.twitter.inject.tests.conversions

import com.twitter.inject.conversions.map._
import com.twitter.inject.Test
import scala.collection.SortedMap

class MapsConversionsTest extends Test {

  test("RichSortedMap#mapKeys") {
    SortedMap(1 -> "a") mapKeys { _.toString } should equal(Map("1" -> "a"))
  }

  test("RichSortedMapOfSortedMap#swapKeys") {
    SortedMap(1 -> SortedMap("a" -> "z")).swapKeys should
      equal(SortedMap("a" -> SortedMap(1 -> "z")))
  }

  test("Rich map of maps#swapKeys") {
    Map(1 -> Map("a" -> "z")).swapKeys should equal(Map("a" -> Map(1 -> "z")))
  }

  test("Map[K, Option[V]]#flattenEntries") {
    Map("a" -> Some(1)).flattenEntries should equal(Map("a" -> 1))
    Map("a" -> None).flattenEntries should equal(Map())
    Map("a" -> None, "b" -> Some(2)).flattenEntries should equal(Map("b" -> 2))
  }

  test("RichMap#mapKeys") {
    Map(1 -> "a") mapKeys {_.toString} should
      equal(Map("1" -> "a"))
  }

  test("RichMap#invert simple") {
    Map(1 -> "a").invert should
      equal(Map("a" -> Iterable(1)))
  }

  test("RichMap#invert complex") {
    Map(1 -> "a", 2 -> "a", 3 -> "b").invert should
      equal(Map("a" -> Iterable(1, 2), "b" -> Iterable(3)))
  }

  test("RichMap#invertSingleValue") {
    Map(1 -> "a", 2 -> "a", 3 -> "b").invertSingleValue should
      equal(Map("a" -> 2, "b" -> 3))
  }

  test("RichMap#filterValues") {
    Map(1 -> "a", 2 -> "a", 3 -> "b") filterValues {_ == "b"} should
      equal(Map(3 -> "b"))
  }

  test("RichMap#filterNotValues") {
    Map(1 -> "a", 2 -> "a", 3 -> "b") filterNotValues {_ == "b"} should
      equal(Map(1 -> "a", 2 -> "a"))
  }

  test("RichMap#filterNotKeys") {
    Map(1 -> "a", 2 -> "a", 3 -> "b") filterNotKeys {_ == 3} should
      equal(Map(1 -> "a", 2 -> "a"))
  }
}