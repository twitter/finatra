package org.apache.kafka.streams.state.internals

object WindowKeySchemaExtractor {
  def extractStoreTimestamp(binaryKey: Array[Byte]): Long = {
    WindowKeySchema.extractStoreTimestamp(binaryKey)
  }

  def extractStoreKeyBytes(binaryKey: Array[Byte]): Array[Byte] = {
    WindowKeySchema.extractStoreKeyBytes(binaryKey)
  }
}
