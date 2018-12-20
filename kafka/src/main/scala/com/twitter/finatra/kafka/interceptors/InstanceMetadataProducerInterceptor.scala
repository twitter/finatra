package com.twitter.finatra.kafka.interceptors

import com.twitter.finatra.kafka.interceptors.InstanceMetadataProducerInterceptor._
import com.twitter.finatra.kafka.utils.ConfigUtils
import java.util
import org.apache.kafka.clients.producer.{ProducerInterceptor, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serdes

object InstanceMetadataProducerInterceptor {
  val KafkaInstanceKeyFlagName = "kafka.instance.key"
  val InstanceKeyHeaderName = "instance_key"
}

/**
 * An interceptor that includes metadata about specific instance in `record` headers.
 *
 * `instance_key` is a configurable header serialized with `Serdes.StringSerde`. The value
 * of the header is configured by `kafka.instance.key` application flag. Only if the flag
 * is set will a header key/value be serialized into the record.
 */
class InstanceMetadataProducerInterceptor extends ProducerInterceptor[Any, Any] {
  private var instanceKey = ""
  private var instanceKeyBytes: Array[Byte] = _
  private val serializer = new Serdes.StringSerde().serializer()

  override def onSend(record: ProducerRecord[Any, Any]): ProducerRecord[Any, Any] = {
    if (instanceKey.nonEmpty) {
      record
        .headers()
        .add(InstanceKeyHeaderName, instanceKeyBytes)
    }
    record
  }

  override def onAcknowledgement(metadata: RecordMetadata, exception: Exception): Unit = {}

  override def close(): Unit = {
    serializer.close()
    instanceKey = ""
    instanceKeyBytes = null
  }

  override def configure(configs: util.Map[String, _]): Unit = {
    instanceKey = ConfigUtils.getConfigOrElse(configs, key = KafkaInstanceKeyFlagName, default = "")
    if (instanceKey.nonEmpty) {
      instanceKeyBytes = serializeInstanceKeyHeader()
    }
  }

  protected def serializeInstanceKeyHeader(): Array[Byte] = {
    serializer.serialize("", instanceKey)
  }
}
