package com.twitter.finatra.streams.partitioning.internal

import java.util
import org.apache.kafka.clients.consumer.{Consumer, ConsumerConfig}
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier

class StaticPartitioningKafkaClientSupplierSupplier(numApplicationInstances: Int)
    extends DefaultKafkaClientSupplier {

  override def getConsumer(config: util.Map[String, AnyRef]): Consumer[Array[Byte], Array[Byte]] = {
    config.put(
      StaticPartitioningStreamAssignor.ApplicationNumInstances,
      numApplicationInstances.toString
    )
    config.put(
      ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
      classOf[StaticPartitioningStreamAssignor].getName
    )
    super.getConsumer(config)
  }
}
