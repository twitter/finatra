package com.twitter.finatra.kafkastreams.utils

import java.util.NoSuchElementException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.KeyValueIterator
import org.rocksdb.RocksIterator

//Inspired by https://github.com/apache/samza/blob/master/samza-kv-rocksdb/src/main/scala/org/apache/samza/storage/kv/RocksDbKeyValueStore.scala#L277
class RocksKeyValueIterator[K, V](
  iterator: RocksIterator,
  keyDeserializer: Deserializer[K],
  valueDeserializer: Deserializer[V],
  storeName: String)
    extends KeyValueIterator[K, V] {

  @volatile
  private var open: Boolean = true

  override def hasNext: Boolean = {
    if (!open) {
      throw new InvalidStateStoreException(s"RocksDB store $storeName has closed")
    }
    iterator.isValid
  }

  override def peekNextKey(): K = {
    if (!hasNext) {
      throw new NoSuchElementException
    }

    keyDeserializer.deserialize("", iterator.key())
  }

  // By virtue of how RocksdbIterator is implemented, the implementation of
  // our iterator is slightly different from standard java iterator next will
  // always point to the current element, when next is called, we return the
  // current element we are pointing to and advance the iterator to the next
  // location (The new location may or may not be valid - this will surface
  // when the next next() call is made, the isValid will fail)
  override def next(): KeyValue[K, V] = {
    if (!hasNext) {
      throw new NoSuchElementException
    }

    val entry = new KeyValue(
      keyDeserializer.deserialize("", iterator.key()),
      valueDeserializer.deserialize("", iterator.value())
    )

    iterator.next()

    entry
  }

  override def close(): Unit = {
    open = false
    iterator.close()
  }
}
