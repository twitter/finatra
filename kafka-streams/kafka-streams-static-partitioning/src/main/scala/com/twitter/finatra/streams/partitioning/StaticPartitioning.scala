package com.twitter.finatra.streams.partitioning

import com.twitter.app.Flag
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.streams.partitioning.internal.StaticPartitioningKafkaClientSupplierSupplier
import org.apache.kafka.streams.KafkaClientSupplier

object StaticPartitioning {
  val PreRestoreSignalingPort = 0 //TODO: Hack to signal our assignor that we are in PreRestore mode
}

trait StaticPartitioning extends KafkaStreamsTwitterServer {

  protected val numApplicationInstances: Flag[Int] =
    flag[Int](
      "kafka.application.num.instances",
      "Total number of instances for static partitioning"
    )

  /* Protected */

  override def kafkaStreamsClientSupplier: KafkaClientSupplier = {
    new StaticPartitioningKafkaClientSupplierSupplier(numApplicationInstances())
  }
}
