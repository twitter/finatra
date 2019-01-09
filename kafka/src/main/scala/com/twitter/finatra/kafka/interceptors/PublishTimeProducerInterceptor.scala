package com.twitter.finatra.kafka.interceptors

import com.google.common.primitives.Longs
import com.twitter.finatra.kafka.interceptors.PublishTimeProducerInterceptor._
import java.util
import org.apache.kafka.clients.producer.{ProducerInterceptor, ProducerRecord, RecordMetadata}
import org.joda.time.DateTimeUtils

object PublishTimeProducerInterceptor {
  val PublishTimeHeaderName = "publish_time"
}

/**
 * An interceptor that puts a header on each Kafka record indicating when the record was published.
 */
class PublishTimeProducerInterceptor extends ProducerInterceptor[Any, Any] {

  override def onSend(record: ProducerRecord[Any, Any]): ProducerRecord[Any, Any] = {
    record
      .headers()
      .add(PublishTimeHeaderName, Longs.toByteArray(DateTimeUtils.currentTimeMillis()))
    record
  }

  override def onAcknowledgement(metadata: RecordMetadata, exception: Exception): Unit = {}

  override def close(): Unit = {}

  override def configure(configs: util.Map[String, _]): Unit = {}
}
