package com.twitter.finatra.kafkastreams

import com.twitter.finatra.kafka.producers.TracingKafkaProducer
import java.util
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.{Consumer, KafkaConsumer}
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.apache.kafka.streams.KafkaClientSupplier
import scala.collection.JavaConverters._

/**
 * An implementation of [[KafkaClientSupplier]] which provides the [[TracingKafkaProducer]]
 * which should enable tracing for producers in Kafka streams.
 */
private[finatra] class TracingKafkaClientSupplier extends KafkaClientSupplier {

  override def getAdminClient(config: util.Map[String, AnyRef]): AdminClient =
    AdminClient.create(config)

  override def getProducer(config: util.Map[String, AnyRef]): Producer[Array[Byte], Array[Byte]] =
    new TracingKafkaProducer(config.asScala.toMap, new ByteArraySerializer, new ByteArraySerializer)

  override def getConsumer(config: util.Map[String, AnyRef]): Consumer[Array[Byte], Array[Byte]] =
    getKafkaConsumer(config)

  override def getRestoreConsumer(
    config: util.Map[String, AnyRef]
  ): Consumer[Array[Byte], Array[Byte]] = getKafkaConsumer(config)

  override def getGlobalConsumer(
    config: util.Map[String, AnyRef]
  ): Consumer[Array[Byte], Array[Byte]] = getKafkaConsumer(config)

  private def getKafkaConsumer(
    config: util.Map[String, AnyRef]
  ): KafkaConsumer[Array[Byte], Array[Byte]] = {
    new KafkaConsumer(config, new ByteArrayDeserializer, new ByteArrayDeserializer)
  }
}
