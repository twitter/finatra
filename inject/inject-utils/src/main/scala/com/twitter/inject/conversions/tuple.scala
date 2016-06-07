package com.twitter.inject.conversions

import scala.collection.{SortedMap, immutable, mutable}
import scala.math.Ordering

object tuple {

  implicit class RichTuples[A, B](val self: Iterable[(A, B)]) extends AnyVal {
    def toKeys: Seq[A] = {
      self.toSeq map { case (key, value) => key}
    }

    def toKeySet: Set[A] = {
      toKeys.toSet
    }

    def toValues: Seq[B] = {
      self.toSeq map { case (key, value) => value}
    }

    def mapValues[C](func: B => C): Seq[(A, C)] = {
      self.toSeq map { case (key, value) =>
        key -> func(value)
      }
    }

    def groupByKey: Map[A, Seq[B]] = {
      val mutableMapBuilder = mutable.Map.empty[A, mutable.Builder[B, Seq[B]]]
      for ((a, b) <- self) {
        val seqBuilder = mutableMapBuilder.getOrElseUpdate(a, immutable.Seq.newBuilder[B])
        seqBuilder += b
      }

      val mapBuilder = immutable.Map.newBuilder[A, Seq[B]]
      for ((k, v) <- mutableMapBuilder) {
        mapBuilder += ((k, v.result()))
      }

      mapBuilder.result()
    }

    def groupByKeyAndReduce(reduceFunc: (B, B) => B): Map[A, B] = {
      groupByKey mapValues { values =>
        values.reduce(reduceFunc)
      }
    }

    def sortByKey(implicit ord: Ordering[A]): Seq[(A, B)] = {
      self.toSeq sortBy { case (key, value) => key}
    }

    def toSortedMap(implicit ord: Ordering[A]): SortedMap[A, B] = {
      SortedMap(self.toSeq: _*)
    }
  }
}
