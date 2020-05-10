package com.twitter.finatra.kafkastreams.internal.admin

import com.twitter.finagle.Service
import com.twitter.finagle.http._
import com.twitter.util.Future
import org.apache.kafka.streams.Topology

private[kafkastreams] object KafkaStreamsTopologyHandler {

  /**
   * Create a service function that prints the kafka topology and formats it in HTML.
   * @param topology Kafka Topology
   * @return HTML formatted properties
   */
  def apply(topology: Topology): Service[Request, Response] = {
    new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val response = Response(Version.Http11, Status.Ok)
        response.setContentType(MediaType.Html)
        val describeHtml =
          s"""
            |<pre>
            |${topology.describe().toString.trim()}
            |</pre>
          """.stripMargin
        ResponseWriter(response)(_.print(describeHtml))
      }
    }
  }
}
