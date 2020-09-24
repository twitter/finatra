package org.apache.kafka.streams.state.internals

import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier

/**
 * A store supplier that can be used to create one or more {@link KeyValueStore KeyValueStore<Bytes, byte[]>} instances of type &lt;Byte, byte[]&gt;.
 *
 * For any stores implementing the {@link KeyValueStore KeyValueStore<Bytes, byte[]>} interface, null value bytes are considered as "not exist". This means:
 *
 * 1. Null value bytes in put operations should be treated as delete.
 * 2. If the key does not exist, get operations should return null value bytes.
 *
 * Note: This class is equivalent to {@link org.apache.kafka.streams.state.KeyValueBytesStoreSupplier} except that the get
 * method returns a RocksDBStore instead of a KeyValueStore
 */
class FinatraRocksDbKeyValueBytesStoreSupplier(
  override val name: String)
    extends KeyValueBytesStoreSupplier {

  override def get: RocksDBStore = RocksDBStoreFactory.create(name, "rocksdb-state")

  override def metricsScope: String = "rocksdb-state"
}
