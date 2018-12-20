package com.twitter.finatra.streams.transformer

/**
 * This Iterator will take an Iterator and split it into subiterators, where each subiterator
 * contains all contiguous elements that have the same span as defined by the getSpan function.
 *
 * For example:
 *
 * Passing in an Iterator(1, 1, 1, 2, 2, 3) with a span function of `identity` will yield:
 * Iterator(
 *   Iterator(1,1,1),
 *   Iterator(2,2),
 *   Iterator(3))
 *
 * If there are multiple elements that have the same span, but they are not contiguous then
 * they will be returned in separate subiterators.
 *
 * For example:
 *
 * Passing in an Iterator(1,2,1,2) with a span function of `identity` will yield:
 * Iterator(
 *   Iterator(1),
 *   Iterator(2),
 *   Iterator(1),
 *   Iterator(2))
 *
 * Contiguous is defined by the Iterator.span function:
 *
 * @see [[scala.collection.Iterator.span]]
 *
 * @param iterator The iterator to split
 * @param getSpanId A function of item to span
 * @tparam T the type of the item
 * @tparam SpanId the type of the span
 */
class MultiSpanIterator[T, SpanId](private var iterator: Iterator[T], getSpanId: T => SpanId)
    extends Iterator[Iterator[T]] {

  override def hasNext: Boolean = {
    iterator.nonEmpty
  }

  override def next(): Iterator[T] = {
    val headItem = iterator.next
    val headSpanId = getSpanId(headItem)

    val (contiguousItems, remainingItems) = iterator.span { currentItem =>
      getSpanId(currentItem) == headSpanId
    }

    // mutate the iterator member to be the remaining items
    iterator = remainingItems

    Iterator(headItem) ++ contiguousItems
  }
}
