package com.twitter.finatra.kafka.serde

trait ReusableDeserialize[T] {
  def deserialize(bytes: Array[Byte], reusable: T): Unit
}
