package com.twitter.finatra.kafkastreams.test

import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.state.KeyValueIterator
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

trait IteratorWithAutoCloseToSeq {

  implicit class RichIteratorWithAutoCloseToSeq[K, V](keyValueIterator: KeyValueIterator[K, V]) {
    def toSeqWithAutoClose: Seq[KeyValue[K, V]] = {
      val keyValueBuffer: ArrayBuffer[KeyValue[K, V]] = new ArrayBuffer[KeyValue[K, V]]

      for {
        keyValue <- keyValueIterator.asScala
      } {
        keyValueBuffer += keyValue
      }

      keyValueIterator.close()
      keyValueBuffer.toSeq
    }
  }
}
