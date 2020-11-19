package com.twitter.inject.conversions

import com.twitter.conversions.TupleOps
import scala.collection.SortedMap
import scala.math.Ordering

@deprecated("Use com.twitter.conversions.TupleOps instead", "2020-11-16")
object tuple {

  implicit class RichTuples[A, B](val self: Iterable[(A, B)]) extends AnyVal {
    @deprecated("Use com.twitter.conversions.TupleOps#toKeys instead", "2020-11-16")
    def toKeys: Seq[A] = TupleOps.toKeys(self)

    @deprecated("Use com.twitter.conversions.TupleOps#toKeys instead", "2020-11-16")
    def toKeySet: Set[A] = TupleOps.toKeySet(self)

    @deprecated("Use com.twitter.conversions.TupleOps#toValues instead", "2020-11-16")
    def toValues: Seq[B] = TupleOps.toValues(self)

    @deprecated("Use com.twitter.conversions.TupleOps#mapValues instead", "2020-11-16")
    def mapValues[C](func: B => C): Seq[(A, C)] =
      TupleOps.mapValues(self, func)

    @deprecated("Use com.twitter.conversions.TupleOps#groupByKey instead", "2020-11-16")
    def groupByKey: Map[A, Seq[B]] =
      TupleOps.groupByKey(self)

    @deprecated("Use com.twitter.conversions.TupleOps#groupByKeyAndReduce instead", "2020-11-16")
    def groupByKeyAndReduce(reduceFunc: (B, B) => B): Map[A, B] =
      TupleOps.groupByKeyAndReduce(self, reduceFunc)

    @deprecated("Use com.twitter.conversions.TupleOps#sortByKey instead", "2020-11-16")
    def sortByKey(implicit ord: Ordering[A]): Seq[(A, B)] =
      TupleOps.sortByKey(self)

    @deprecated("Use com.twitter.conversions.TupleOps#toSortedMap instead", "2020-11-16")
    def toSortedMap(implicit ord: Ordering[A]): SortedMap[A, B] =
      TupleOps.toSortedMap(self)
  }
}
