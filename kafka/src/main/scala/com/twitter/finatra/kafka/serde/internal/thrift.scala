package com.twitter.finatra.kafka.serde.internal

import java.util

import com.twitter.scrooge.{
  CompactThriftSerializer,
  ThriftStruct,
  ThriftStructCodec,
  ThriftStructSerializer
}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import org.apache.thrift.protocol.TBinaryProtocol

import scala.util.Try

private[serde] abstract class AbstractScroogeSerDe[T <: ThriftStruct: Manifest] extends Serde[T] {
  private[kafka] val thriftStructSerializer: ThriftStructSerializer[T] = {
    val clazz = manifest[T].runtimeClass.asInstanceOf[Class[T]]
    val codec = constructCodec(clazz)

    constructThriftStructSerializer(clazz, codec)
  }

  private val _deserializer = new Deserializer[T] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = {}

    override def deserialize(topic: String, data: Array[Byte]): T = {
      if (data == null) {
        null.asInstanceOf[T]
      } else {
        thriftStructSerializer.fromBytes(data)
      }
    }
  }

  private val _serializer = new Serializer[T] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def serialize(topic: String, data: T): Array[Byte] = {
      if (data == null) {
        null
      } else {
        thriftStructSerializer.toBytes(data)
      }
    }

    override def close(): Unit = {}
  }

  /* Public */

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserializer: Deserializer[T] = {
    _deserializer
  }

  override def serializer: Serializer[T] = {
    _serializer
  }

  /**
   * Subclasses should implement this method and provide a concrete ThriftStructSerializer
   */
  protected[this] def constructThriftStructSerializer(
    thriftStructClass: Class[T],
    thriftStructCodec: ThriftStructCodec[T]
  ): ThriftStructSerializer[T]

  /* Public */
  private[this] def constructCodec(thriftStructClass: Class[T]): ThriftStructCodec[T] =
    codecForNormal(thriftStructClass)
      .orElse(codecForUnion(thriftStructClass))
      .get

  /**
   * For unions, we split on $ after the dot.
   * this is costly, but only done once per Class
   */
  private[this] def codecForUnion(maybeUnion: Class[T]): Try[ThriftStructCodec[T]] =
    Try(
      getObject(
        Class.forName(
          maybeUnion.getName.reverse.dropWhile(_ != '$').reverse,
          true,
          maybeUnion.getClassLoader
        )
      )
    ).map(_.asInstanceOf[ThriftStructCodec[T]])

  private[this] def codecForNormal(thriftStructClass: Class[T]): Try[ThriftStructCodec[T]] =
    Try(
      getObject(
        Class.forName(thriftStructClass.getName + "$", true, thriftStructClass.getClassLoader)
      )
    ).map(_.asInstanceOf[ThriftStructCodec[T]])

  private def getObject(companionClass: Class[_]): AnyRef =
    companionClass.getField("MODULE$").get(null)
}

private[serde] class ThriftSerDe[T <: ThriftStruct: Manifest] extends AbstractScroogeSerDe[T] {
  protected[this] override def constructThriftStructSerializer(
    thriftStructClass: Class[T],
    thriftStructCodec: ThriftStructCodec[T]
  ): ThriftStructSerializer[T] = {
    new ThriftStructSerializer[T] {
      override val protocolFactory = new TBinaryProtocol.Factory
      override def codec: ThriftStructCodec[T] = thriftStructCodec
    }
  }
}

private[serde] class CompactThriftSerDe[T <: ThriftStruct: Manifest]
    extends AbstractScroogeSerDe[T] {
  override protected[this] def constructThriftStructSerializer(
    thriftStructClass: Class[T],
    thriftStructCodec: ThriftStructCodec[T]
  ): ThriftStructSerializer[T] = {
    new CompactThriftSerializer[T] {
      override def codec: ThriftStructCodec[T] = thriftStructCodec
    }
  }
}
