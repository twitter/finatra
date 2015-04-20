package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.tuple._
import scala.collection.{SortedMap, immutable, mutable}

object map {

  implicit class RichMap[K, V](val wrappedMap: Map[K, V]) extends AnyVal {
    def mapKeys[T](func: K => T): Map[T, V] = {
      for ((k, v) <- wrappedMap) yield {
        func(k) -> v
      }
    }

    def invert: Map[V, Seq[K]] = {
      val invertedMapWithBuilderValues = mutable.Map.empty[V, mutable.Builder[K, Seq[K]]]
      for ((k, v) <- wrappedMap) {
        val valueBuilder = invertedMapWithBuilderValues.getOrElseUpdate(v, Seq.newBuilder[K])
        valueBuilder += k
      }

      val invertedMap = immutable.Map.newBuilder[V, Seq[K]]
      for ((k, valueBuilder) <- invertedMapWithBuilderValues) {
        invertedMap += (k -> valueBuilder.result)
      }

      invertedMap.result()
    }

    def invertSingleValue: Map[V, K] = {
      wrappedMap map {_.swap}
    }

    def filterValues(func: V => Boolean): Map[K, V] = {
      wrappedMap filter { case (_, value) =>
        func(value)
      }
    }

    def filterNotValues(func: V => Boolean): Map[K, V] = {
      filterValues(!func(_))
    }

    def filterNotKeys(func: K => Boolean): Map[K, V] = {
      wrappedMap.filterKeys(!func(_))
    }

    def toSortedMap(implicit ordering: Ordering[K]) = {
      SortedMap[K, V]() ++ wrappedMap
    }
  }

  // TODO Investigate why this fails as a Value class
  /*
    [error] overriding method applyOrElse in trait PartialFunction of type [A1 <: (K, Option[V]), B1 >: (K, V)](x: A1, default: A1 => B1)B1;
    [error]  method applyOrElse has incompatible type
    [error]     def flattenEntries: Map[K, V] = wrappedMap collect {^
   */
  implicit class RichOptionValueMap[K, V](wrappedMap: Map[K, Option[V]]) {
    def flattenEntries: Map[K, V] = wrappedMap collect {
      case (key, Some(value)) =>
        key -> value
    }
  }

  implicit class RichMapOfMaps[A, B, C](val wrapped: scala.collection.Map[A, scala.collection.Map[B, C]]) extends AnyVal {

  /**
     * Swap the keys between the inner and outer maps
     * Input: Map[A,Map[B,C]] Output: Map[B, Map[A,C]]
     */
    //TODO: Optimize
    def swapKeys: Map[B, Map[A, C]] = {
      val entries = for {
        (a, bc) <- wrapped.toSeq
        (b, c) <- bc
      } yield b -> (a -> c)

      entries.groupByKey mapValues {_.toMap}
    }
  }

  implicit class RichSortedMapOfSortedMaps[A, B, C](val wrapped: SortedMap[A, SortedMap[B, C]]) extends AnyVal {

    /**
     * Swap the keys between the inner and outer maps
     * Input: Map[A,Map[B,C]] Output: Map[B, Map[A,C]]
     */
    def swapKeys(implicit orderingA: Ordering[A], orderingB: Ordering[B]): SortedMap[B, SortedMap[A, C]] = {
      wrapped.asInstanceOf[scala.collection.Map[A, scala.collection.Map[B, C]]].
        swapKeys.toSortedMap mapValues {_.toSortedMap}
    }
  }

  implicit class RichSortedMap[K, V](val wrappedMap: SortedMap[K, V]) extends AnyVal {
    def mapKeys[T](func: K => T)(implicit ordering: Ordering[T]): SortedMap[T, V] = {
      (for ((k, v) <- wrappedMap) yield {
        func(k) -> v
      }).toSortedMap
    }
  }

}