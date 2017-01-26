package com.twitter.inject.tests.conversions

import com.twitter.inject.conversions.tuple._
import com.twitter.inject.Test

class TuplesConversionsTest extends Test {

  val tuples = Seq(1 -> "bob", 2 -> "sally")
  
  test("RichTuple#toKeys") {
    tuples.toKeys should equal(Seq(1, 2))
  }
  test("RichTuple#toKeySet") {
    tuples.toKeySet should equal(Set(1, 2))
  }
  test("RichTuple#toValues") {
    tuples.toValues should equal(Seq("bob", "sally"))
  }
  test("RichTuple#mapValues") {
    tuples.mapValues {_.length} should equal(Seq(1 -> 3, 2 -> 5))
  }
  test("RichTuple#groupByKey") {
    val multiTuples = Seq(1 -> "a", 1 -> "b", 2 -> "ab")
    multiTuples.groupByKey should equal(Map(2-> Seq("ab"), 1 -> Seq("a", "b")))
  }
  test("RichTuple#groupByKeyAndReduce") {
    val multiTuples = Seq(1 -> 5, 1 -> 6, 2 -> 7)
    multiTuples.groupByKeyAndReduce(_ + _) should equal(Map(2-> 7, 1 -> 11))
  }
}
