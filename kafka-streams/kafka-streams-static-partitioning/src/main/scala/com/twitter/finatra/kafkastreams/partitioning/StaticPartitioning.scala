package com.twitter.finatra.kafkastreams.partitioning

import com.twitter.app.Flag
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.partitioning.internal.StaticPartitioningKafkaClientSupplierSupplier
import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import org.apache.kafka.streams.KafkaClientSupplier
import scala.util.control.NonFatal

object StaticPartitioning {
  val PreRestoreSignalingPort = 0 //TODO: Hack to signal our assignor that we are in PreRestore mode

  def parseShardId(applicationServerHost: String): ServiceShardId = {
    val firstPeriodIndex = applicationServerHost.indexOf('.')

    val shardId =
      try {
        applicationServerHost.substring(0, firstPeriodIndex).toInt
      } catch {
        case NonFatal(e) =>
          throw new Exception(
            "Finatra Kafka Stream's StaticPartitioning functionality requires the " +
              "'kafka.application.server' flag value to be specified as '<kafka.current.shard>.<unused_hostname>:<unused_port>" +
              " where unused_hostname can be empty and unused_port must be > 0. As an example, to configure the server" +
              " that represents shard #5, you can set 'kafka.application.server=5.:80'. In this example, port 80 is unused and does not" +
              " need to represent an actual open port"
          )
      }

    ServiceShardId(shardId)
  }
}

trait StaticPartitioning extends KafkaStreamsTwitterServer {

  protected val numApplicationInstances: Flag[Int] =
    flag[Int](
      "kafka.application.num.instances",
      "Total number of instances for static partitioning"
    )

  /* Protected */

  override def kafkaStreamsClientSupplier: KafkaClientSupplier = {
    new StaticPartitioningKafkaClientSupplierSupplier(
      numApplicationInstances(),
      applicationServerConfig())
  }
}
