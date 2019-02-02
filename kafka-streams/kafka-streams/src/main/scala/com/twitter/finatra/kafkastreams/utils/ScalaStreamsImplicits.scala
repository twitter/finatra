package com.twitter.finatra.kafkastreams.utils

import org.apache.kafka.streams.kstream.{Transformer, TransformerSupplier, KStream => KStreamJ}
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.scala.kstream.{KStream => KStreamS}
import org.apache.kafka.streams.scala.{StreamsBuilder => StreamsBuilderS}
import org.apache.kafka.streams.{KeyValue, StreamsBuilder => StreamsBuilderJ}
import scala.language.implicitConversions

/**
 * Implicit conversions to enhance the Scala Kafka Streams DSL
 */
trait ScalaStreamsImplicits {

  /* ---------------------------------------- */
  implicit class StreamsBuilderConversions(streamsBuilder: StreamsBuilderJ) {

    def asScala: StreamsBuilderS = {
      new StreamsBuilderS(streamsBuilder)
    }
  }

  /* ---------------------------------------- */
  implicit class KStreamJConversions[K, V](kStream: KStreamJ[K, V]) {

    def asScala: KStreamS[K, V] = {
      new KStreamS(kStream)
    }
  }

  /* ---------------------------------------- */
  // Helper until we move to Scala 2.12 which will use SAM conversion to implement the TransformerSupplier interface
  implicit def transformerFunctionToSupplier[K, V, K1, V1](
    transformerFactory: () => Transformer[K, V, (K1, V1)]
  ): TransformerSupplier[K, V, KeyValue[K1, V1]] = {
    new TransformerSupplier[K, V, KeyValue[K1, V1]] {
      override def get(): Transformer[K, V, KeyValue[K1, V1]] = {
        new Transformer[K, V, KeyValue[K1, V1]] {
          private val transformer = transformerFactory()

          override def init(context: ProcessorContext): Unit = {
            transformer.init(context)
          }

          override def transform(key: K, value: V): KeyValue[K1, V1] = {
            transformer.transform(key, value) match {
              case (k1, v1) => KeyValue.pair(k1, v1)
              case _ => null
            }
          }

          override def close(): Unit = {
            transformer.close()
          }
        }
      }
    }
  }

  /* ---------------------------------------- */
  implicit class KStreamSConversions[K, V](inner: KStreamS[K, V]) {

    // Helper until we move to Scala 2.12 which will use SAM conversion to implement the TransformerSupplier interface
    def transformS[K1, V1](
      transformerFactory: () => Transformer[K, V, (K1, V1)],
      stateStoreNames: String*
    ): KStreamS[K1, V1] = {
      val transformerSupplierJ: TransformerSupplier[K, V, KeyValue[K1, V1]] =
        new TransformerSupplier[K, V, KeyValue[K1, V1]] {
          override def get(): Transformer[K, V, KeyValue[K1, V1]] = {
            val transformer = transformerFactory()
            new Transformer[K, V, KeyValue[K1, V1]] {
              override def transform(key: K, value: V): KeyValue[K1, V1] = {
                transformer.transform(key, value) match {
                  case (k1, v1) => KeyValue.pair(k1, v1)
                  case _ => null
                }
              }

              override def init(context: ProcessorContext): Unit = transformer.init(context)

              override def close(): Unit = transformer.close()
            }
          }
        }
      new KStreamS(inner.inner.transform(transformerSupplierJ, stateStoreNames: _*))
    }

    def filterValues(f: V => Boolean): KStreamS[K, V] = {
      inner.filter { (key: K, value: V) =>
        f(value)
      }
    }
  }
}
