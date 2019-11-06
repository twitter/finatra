/*
 * Copyright (c) 2016 Fred Cecilia, Valentin Kasas, Olivier Girardot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
//Derived from: https://github.com/aseigneurin/kafka-streams-scala
package com.twitter.finatra.kafka.serde.internal

import java.util
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer, _}

object IntSerde extends BaseSerde[Int] {
  private val innerSerializer = new IntegerSerializer
  private val innerDeserializer = new IntegerDeserializer

  final override def serialize(topic: String, data: Int): Array[Byte] =
    innerSerializer.serialize(topic, data)

  final override def deserialize(topic: String, data: Array[Byte]): Int =
    innerDeserializer.deserialize(topic, data)

}

object LongSerde extends BaseSerde[Long] {
  private val innerSerializer = new LongSerializer
  private val innerDeserializer = new LongDeserializer

  final override def serialize(topic: String, data: Long): Array[Byte] =
    innerSerializer.serialize(topic, data)

  final override def deserialize(topic: String, data: Array[Byte]): Long =
    innerDeserializer.deserialize(topic, data)

}

object DoubleSerde extends BaseSerde[Double] {
  private val innerSerializer = new DoubleSerializer
  private val innerDeserializer = new DoubleDeserializer

  final override def serialize(topic: String, data: Double): Array[Byte] =
    innerSerializer.serialize(topic, data)

  final override def deserialize(topic: String, data: Array[Byte]): Double =
    innerDeserializer.deserialize(topic, data)

}

abstract class BaseSerde[T] extends Serde[T] {

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def serializer = BaseSerializer(serialize)

  override def deserializer = BaseDeserializer(deserialize)

  def serialize(topic: String, data: T): Array[Byte]

  def serializeBytes(data: T): Array[Byte] = serialize("", data)

  def deserialize(topic: String, data: Array[Byte]): T

  def deserializeBytes(data: Array[Byte]): T = deserialize("", data)
}

abstract class BaseSerializer[T] extends Serializer[T] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}
}

object BaseSerializer {
  def apply[T](func: (String, T) => Array[Byte]): BaseSerializer[T] = new BaseSerializer[T] {
    final override def serialize(topic: String, data: T): Array[Byte] = {
      if (data == null) {
        null
      } else {
        func(topic, data)
      }
    }
  }
}

abstract class BaseDeserializer[T] extends Deserializer[T] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}
}

object BaseDeserializer {
  def apply[T](func: (String, Array[Byte]) => T): BaseDeserializer[T] = new BaseDeserializer[T] {
    final override def deserialize(topic: String, data: Array[Byte]): T = {
      if (data == null) {
        null.asInstanceOf[T]
      } else {
        func(topic, data)
      }
    }
  }
}
