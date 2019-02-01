package org.apache.kafka.streams.state.internals

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.state.KeyValueStore

class InMemoryKeyValueFlushingStoreBuilder[K, V](
  name: String,
  keySerde: Serde[K],
  valueSerde: Serde[V],
  time: Time = Time.SYSTEM)
    extends AbstractStoreBuilder[K, V, KeyValueStore[K, V]](name, keySerde, valueSerde, time) {

  override def build(): KeyValueStore[K, V] = {
    val inMemoryKeyValueStore = new InMemoryKeyValueStore[K, V](name, keySerde, valueSerde)
    val inMemoryFlushingKeyValueStore =
      new InMemoryKeyValueFlushingLoggedStore[K, V](inMemoryKeyValueStore, keySerde, valueSerde)
    new MeteredKeyValueStore[K, V](inMemoryFlushingKeyValueStore, "in-memory-state", time)
  }
}
