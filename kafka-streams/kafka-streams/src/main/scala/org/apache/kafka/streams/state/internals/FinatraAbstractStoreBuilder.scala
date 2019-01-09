package org.apache.kafka.streams.state.internals

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.processor.StateStore

/* Note: To avoid code duplication for now, this class is created for access to package protected AbstractStoreBuilder */
abstract class FinatraAbstractStoreBuilder[K, V, T <: StateStore](
  name: String,
  keySerde: Serde[K],
  valueSerde: Serde[V],
  time: Time)
    extends AbstractStoreBuilder[K, V, T](name, keySerde, valueSerde, time)
