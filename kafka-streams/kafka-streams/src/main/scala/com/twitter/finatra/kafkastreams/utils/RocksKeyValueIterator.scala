package com.twitter.finatra.kafkastreams.utils

import java.util.NoSuchElementException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.KeyValueIterator
import org.rocksdb.RocksIterator

class RocksKeyValueIterator[K, V](
  iterator: RocksIterator,
  keyDeserializer: Deserializer[K],
  valueDeserializer: Deserializer[V],
  storeName: String)
    extends KeyValueIterator[K, V] {

  private var open: Boolean = true

  override def hasNext: Boolean = {
    if (!open) throw new InvalidStateStoreException(s"RocksDB store $storeName has closed")
    iterator.isValid
  }

  override def peekNextKey(): K = {
    if (!hasNext) throw new NoSuchElementException
    keyDeserializer.deserialize("", iterator.key())
  }

  override def next(): KeyValue[K, V] = {
    if (!hasNext) throw new NoSuchElementException
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
