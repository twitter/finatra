package com.twitter.finatra.kafkastreams.internal.admin

import com.twitter.server.AdminHttpServer
import com.twitter.server.AdminHttpServer.Route
import java.util.Properties
import org.apache.kafka.streams.Topology

private[kafkastreams] object AdminRoutes {
  def apply(properties: Properties, topology: Topology): Seq[Route] = {
    // Kafka Properties
    Seq(
      AdminHttpServer.mkRoute(
        path = "/admin/kafka/streams/properties",
        handler = KafkaStreamsPropertiesHandler(properties),
        alias = "kafkaStreamsProperties",
        group = Some("Kafka"),
        includeInIndex = true
      ),
      // Kafka Topology
      AdminHttpServer.mkRoute(
        path = "/admin/kafka/streams/topology",
        handler = KafkaStreamsTopologyHandler(topology),
        alias = "kafkaStreamsTopology",
        group = Some("Kafka"),
        includeInIndex = true
      )
    )
  }
}
