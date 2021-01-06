package com.twitter.finatra.kafkastreams.internal.admin

import com.twitter.finagle.Service
import com.twitter.finagle.http._
import com.twitter.util.Future
import java.util.Properties
import scala.collection.JavaConverters._

private[kafkastreams] object KafkaStreamsPropertiesHandler {

  /**
   * Create a service function that extracts the key/value of kafka properties and formats it in
   * HTML.
   * @param properties Kafka Properties
   * @return HTML formatted properties
   */
  def apply(properties: Properties): Service[Request, Response] = {
    new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val response = Response(Version.Http11, Status.Ok)
        response.setContentType(MediaType.Html)
        val sortedProperties = properties
          .propertyNames().asScala.map { property =>
            s"$property=${properties.get(property)}"
          }.toSeq.sorted.mkString("\n")
        ResponseWriter(response)(_.print(s"<pre>$sortedProperties</pre>"))
      }
    }
  }
}
