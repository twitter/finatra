package com.twitter.finatra.kafkastreams.partitioning.internal

import java.util
import com.twitter.finatra.kafkastreams.partitioning.StaticPartitioning
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.utils.Utils
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier

class StaticPartitioningKafkaClientSupplierSupplier(
  numApplicationInstances: Int,
  serverConfig: String)
    extends DefaultKafkaClientSupplier {

  override def getConsumer(config: util.Map[String, AnyRef]): Consumer[Array[Byte], Array[Byte]] = {
    val applicationServerHost = Utils.getHost(serverConfig)

    val serviceShardId = StaticPartitioning.parseShardId(applicationServerHost)

    config.put(
      "group.instance.id",
      serviceShardId.id.toString
    )

    super.getConsumer(config)
  }
}
