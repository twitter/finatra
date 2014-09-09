package com.twitter.finatra.conversions


object tuples {

  class RichTuples[A, B](tuples: Seq[(A, B)]) {
    def toKeys: Seq[A] = {
      tuples map { case (key, value) => key}
    }

    def toKeySet: Set[A] = {
      toKeys.toSet
    }

    def toValues: Seq[B] = {
      tuples map { case (key, value) => value}
    }

    def mapValues[C](func: B => C): Seq[(A, C)] = {
      tuples map { case (key, value) =>
        key -> func(value)
      }
    }

    // go/jira/DATAAPI-198 - optimize w/ builder
    def toMultiMap: Map[A, Seq[B]] = {
      val startingMap = Map[A, Seq[B]]().withDefaultValue(Seq())
      tuples.foldLeft(startingMap) { case (map, (key, value)) =>
        val existingValue = map(key)
        val newValue = value +: existingValue
        map.updated(key, newValue)
      }
    }
  }

  implicit def enrichTuples[A, B](tuples: Seq[(A, B)]): RichTuples[A, B] = new RichTuples[A, B](tuples)
}
