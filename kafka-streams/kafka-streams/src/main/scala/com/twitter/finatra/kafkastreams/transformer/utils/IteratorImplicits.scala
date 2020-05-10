package com.twitter.finatra.kafkastreams.transformer.utils

import org.agrona.collections.{Hashing, Object2ObjectHashMap}
import org.apache.kafka.streams.state.KeyValueIterator
import scala.collection.JavaConverters._

trait IteratorImplicits {

  implicit class RichIterator[T](iterator: Iterator[T]) {

    final def multiSpan[SpanId](getSpanId: T => SpanId): Iterator[Iterator[T]] = {
      new MultiSpanIterator(iterator, getSpanId)
    }
  }

  /* ------------------------------------------ */
  implicit class RichKeyValueIterator[K, V](keyValueIterator: KeyValueIterator[K, V]) {

    final def peekNextKeyOpt: Option[K] = {
      if (keyValueIterator.hasNext) {
        Some(keyValueIterator.peekNextKey())
      } else {
        None
      }
    }

    final def keys: Iterator[K] = {
      new Iterator[K] {
        override def hasNext: Boolean = {
          keyValueIterator.hasNext
        }

        override def next(): K = {
          keyValueIterator.next().key
        }
      }
    }

    final def values: Iterator[V] = {
      new Iterator[V] {
        override def hasNext: Boolean = {
          keyValueIterator.hasNext
        }

        override def next(): V = {
          keyValueIterator.next().value
        }
      }
    }

    //NOTE: If sharedMap is set to true, a shared mutable map is returned in the iterator. You must immediately use the map's contents or copy the map otherwise the map's
    //      contents will change after each iterator of the iterator!
    final def groupBy[PrimaryKey, SecondaryKey, MappedValue](
      primaryKey: K => PrimaryKey,
      secondaryKey: K => SecondaryKey,
      mapValue: V => MappedValue,
      filterSecondaryKey: (SecondaryKey => Boolean) = (_: SecondaryKey) => true,
      sharedMap: Boolean = false
    ): Iterator[(PrimaryKey, scala.collection.Map[SecondaryKey, MappedValue])] = {
      new Iterator[(PrimaryKey, scala.collection.Map[SecondaryKey, MappedValue])] {
        final override def hasNext: Boolean = keyValueIterator.hasNext

        final override def next(): (PrimaryKey, scala.collection.Map[SecondaryKey, MappedValue]) = {
          val secondaryKeyMap = getSecondaryMap()

          val currentPartition = primaryKey(keyValueIterator.peekNextKey())
          while (keyValueIterator.hasNext && primaryKey(
              keyValueIterator.peekNextKey) == currentPartition) {
            val entry = keyValueIterator.next()
            val secondaryKeyToAdd = secondaryKey(entry.key)
            if (filterSecondaryKey(secondaryKeyToAdd)) {
              secondaryKeyMap.put(
                secondaryKeyToAdd.asInstanceOf[Any],
                mapValue(entry.value).asInstanceOf[Any]
              )
            }
          }
          currentPartition -> secondaryKeyMap.asScala
        }

        private var reusableSharedMap: Object2ObjectHashMap[SecondaryKey, MappedValue] = _

        private def getSecondaryMap(): Object2ObjectHashMap[SecondaryKey, MappedValue] = {
          if (sharedMap) {
            if (reusableSharedMap == null) {
              reusableSharedMap = createMap()
            } else {
              reusableSharedMap.clear()
            }
            reusableSharedMap
          } else {
            createMap()
          }
        }

        private def createMap() = {
          new Object2ObjectHashMap[SecondaryKey, MappedValue](16, Hashing.DEFAULT_LOAD_FACTOR)
        }
      }
    }

    final def take(num: Int): Iterator[(K, V)] = {
      keyValueIterator.asScala
        .take(num)
        .map(keyValue => (keyValue.key, keyValue.value))
    }
  }

}
