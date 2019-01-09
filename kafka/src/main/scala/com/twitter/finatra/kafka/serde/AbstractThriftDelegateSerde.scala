package com.twitter.finatra.kafka.serde

import com.twitter.scrooge.ThriftStruct

abstract class AbstractThriftDelegateSerde[T, ThriftType <: ThriftStruct: Manifest]
    extends AbstractSerde[T] {

  private val thriftStructSerializer = ScalaSerdes.Thrift[ThriftType].thriftStructSerializer

  def toThrift(value: T): ThriftType

  def fromThrift(thrift: ThriftType): T

  final override def serialize(obj: T): Array[Byte] = {
    thriftStructSerializer.toBytes(toThrift(obj))
  }

  final override def deserialize(bytes: Array[Byte]): T = {
    fromThrift(thriftStructSerializer.fromBytes(bytes))
  }
}
