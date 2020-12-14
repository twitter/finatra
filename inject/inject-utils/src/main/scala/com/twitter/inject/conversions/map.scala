package com.twitter.inject.conversions

import com.twitter.conversions.MapOps
import com.twitter.inject.conversions.tuple._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.compat.immutable.ArraySeq
import scala.collection.{SortedMap, immutable}

object map {

  @deprecated("Use com.twitter.conversions.MapOps instead", "2020-11-16")
  implicit class RichMap[K, V](val self: Map[K, V]) extends AnyVal {
    @deprecated("Use com.twitter.conversions.MapOps#mapKeys instead", "2020-11-16")
    def mapKeys[T](func: K => T): Map[T, V] = MapOps.mapKeys(self, func)

    @deprecated("Use com.twitter.conversions.MapOps#invert instead", "2020-11-16")
    def invert: Map[V, Seq[K]] = MapOps.invert(self)

    @deprecated("Use com.twitter.conversions.MapOps#invertSingleValue instead", "2020-11-16")
    def invertSingleValue: Map[V, K] = MapOps.invertSingleValue(self)

    @deprecated("Use com.twitter.conversions.MapOps#filterValues instead", "2020-11-16")
    def filterValues(func: V => Boolean): Map[K, V] =
      MapOps.filterValues(self, func)

    @deprecated("Use com.twitter.conversions.MapOps#filterNotValues instead", "2020-11-16")
    def filterNotValues(func: V => Boolean): Map[K, V] =
      MapOps.filterNotValues(self, func)

    @deprecated("Use com.twitter.conversions.MapOps#filterNotKeys instead", "2020-11-16")
    def filterNotKeys(func: K => Boolean): Map[K, V] =
      MapOps.filterNotKeys(self, func)

    @deprecated("Use com.twitter.conversions.MapOps#toSortedMap instead", "2020-11-16")
    def toSortedMap(implicit ordering: Ordering[K]): SortedMap[K, V] =
      MapOps.toSortedMap(self)

  }

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
  }
}
