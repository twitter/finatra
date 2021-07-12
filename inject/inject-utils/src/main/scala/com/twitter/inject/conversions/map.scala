package com.twitter.inject.conversions

import com.twitter.conversions.TupleOps._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.compat.immutable.ArraySeq
import scala.collection.{SortedMap, immutable}

object map {

  implicit class RichJavaMap[K, V](val self: java.util.Map[K, V]) extends AnyVal {
    import scala.collection.JavaConverters._

    def toOrderedMap: Map[K, V] = {
      val entries = ArraySeq.newBuilder[(K, V)]
      self.asScala.foreach(entries += _)
      immutable.ListMap(entries.result(): _*)
    }
  }

  implicit class RichOptionValueMap[K, V](val self: Map[K, Option[V]]) extends AnyVal {
    def flattenEntries: Map[K, V] = self.collect {
      case (key, Some(value)) =>
        key -> value
    }
  }

  implicit class RichMapOfMaps[A, B, C](
    val self: scala.collection.Map[A, scala.collection.Map[B, C]])
      extends AnyVal {

    /**
     * Swap the keys between the inner and outer maps
     * Input: `Map[A,Map[B,C]]` Output: `Map[B, Map[A,C]]`
     */
    //TODO: Optimize
    def swapKeys: Map[B, Map[A, C]] = {
      val entries = for {
        (a, bc) <- self.toSeq
        (b, c) <- bc
      } yield b -> (a -> c)

      // use map instead of mapValues(deprecated since 2.13.0) for cross-building.
      entries.groupByKey.map { case (key, value) => key -> value.toMap }
    }
  }

  implicit class RichSortedMapOfSortedMaps[A, B, C](val self: SortedMap[A, SortedMap[B, C]])
      extends AnyVal {

    /**
     * Swap the keys between the inner and outer maps
     * Input: `Map[A,Map[B,C]]` Output: `Map[B, Map[A,C]]`
     */
    def swapKeys(
      implicit orderingA: Ordering[A],
      orderingB: Ordering[B]
    ): SortedMap[B, SortedMap[A, C]] = {
      self
        .asInstanceOf[scala.collection.Map[A, scala.collection.Map[B, C]]]
        .swapKeys
        // use map instead of mapValues(deprecated since 2.13.0) for cross-building.
        .toSortedMap.map { case (key, value) => key -> value.toSortedMap }
    }
  }

  implicit class RichSortedMap[K, V](val self: SortedMap[K, V]) extends AnyVal {
    def mapKeys[T](func: K => T)(implicit ordering: Ordering[T]): SortedMap[T, V] = {
      (for ((k, v) <- self) yield {
        func(k) -> v
      }).toSortedMap
    }
  }

  implicit class RichConcurrentMap[A, B](val map: ConcurrentHashMap[A, B]) extends AnyVal {
    def atomicGetOrElseUpdate(key: A, op: => B): B = {
      map.computeIfAbsent(
        key,
        new java.util.function.Function[A, B]() {
          override def apply(key: A): B = {
            op
          }
        })
    }

    def getOption(key: A): Option[B] = {
      val value = map.get(key)
      if (value == null) None
      else Some(value)
    }
  }
}
