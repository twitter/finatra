package com.twitter.finatra.kafkastreams.transformer.utils

import com.twitter.inject.Test

class MultiSpanIteratorTest extends Test {

  test("test spanning function works, all items to the same span") {
    val spanningIterator = new MultiSpanIterator[Int, Int](Seq(1, 1, 1, 2, 2, 3).toIterator, x => 0)
    assertSpanningIterator(spanningIterator, Seq(Seq(1, 1, 1, 2, 2, 3)))
  }

  test("1,1,1") {
    val spanningIterator = new MultiSpanIterator[Int, Int](Seq(1, 1, 1).toIterator, identity)
    assertSpanningIterator(spanningIterator, Seq(Seq(1, 1, 1)))
  }

  test("1") {
    val spanningIterator = new MultiSpanIterator[Int, Int](Seq(1).toIterator, identity)
    assertSpanningIterator(spanningIterator, Seq(Seq(1)))
  }

  test("empty") {
    val spanningIterator = new MultiSpanIterator[Int, Int](Seq().toIterator, identity)
    assertSpanningIterator(spanningIterator, Seq())
  }

  test("1,1,1,2,2,3") {
    val spanningIterator =
      new MultiSpanIterator[Int, Int](Seq(1, 1, 1, 2, 2, 3).toIterator, identity)
    assertSpanningIterator(spanningIterator, Seq(Seq(1, 1, 1), Seq(2, 2), Seq(3)))
  }

  test("1,2,1,2") {
    val spanningIterator = new MultiSpanIterator[Int, Int](Seq(1, 2, 1, 2).toIterator, identity)
    assertSpanningIterator(spanningIterator, Seq(Seq(1), Seq(2), Seq(1), Seq(2)))
  }

  /* Private */

  private def assertSpanningIterator[T](
    iterator: Iterator[Iterator[T]],
    expected: Seq[Seq[T]]
  ): Unit = {
    assert(iterator.map(_.toSeq).toSeq == expected)
  }
}
