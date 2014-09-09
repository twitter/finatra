package com.twitter.finatra.conversions

import scala.collection.{immutable, mutable}

object maps {

  class RichMap[K, V](wrappedMap: Map[K, V]) {
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
      wrappedMap filter { case (key, value) =>
        func(value)
      }
    }
  }

  implicit def mapToRichMap[K, V](map: Map[K, V]): RichMap[K, V] = new RichMap[K, V](map)
}