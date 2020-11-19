package com.twitter.inject.tests.conversions

import com.twitter.inject.conversions.map._
import com.twitter.inject.Test
import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.collection.SortedMap
import scala.collection.immutable.ListMap

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

  test("RichJavaMap#toOrderedMap") {
    val treeMap = new java.util.TreeMap[String, Int]()
    treeMap.put("c", 3)
    treeMap.put("a", 2)
    treeMap.put("b", 1)

    treeMap.toOrderedMap should equal(ListMap("a" -> 2, "b" -> 1, "c" -> 3))
    treeMap.toOrderedMap.toSeq should equal(Seq(("a", 2), ("b", 1), ("c", 3)))

    val orderedMap = new util.LinkedHashMap[String, Int]()
    orderedMap.put("a", 3)
    orderedMap.put("b", 2)
    orderedMap.put("c", 1)
    orderedMap.put("e", 4)
    orderedMap.put("d", 5)

    orderedMap.toOrderedMap should equal(ListMap("a" -> 3, "b" -> 2, "c" -> 1, "e" -> 4, "d" -> 5))
    orderedMap.toOrderedMap.toSeq should equal(
      Seq(("a", 3), ("b", 2), ("c", 1), ("e", 4), ("d", 5)))
  }

  test("RichConcurrentMap#atomicGetOrElseUpdate") {
    val map = new ConcurrentHashMap[String, Int]()
    map.atomicGetOrElseUpdate("1", 1) should equal(1)
    map.get("1") should equal(1)
    map.atomicGetOrElseUpdate("1", 2) should equal(1)
    map.get("1") should equal(1)
  }
}
