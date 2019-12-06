package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.MediaType
import scala.collection.JavaConverters._

object WriterResponse {
  /** For usage from Scala. Java users should use the method below. */
  def apply(
    contentType: String = MediaType.OctetStream,
    body: Any,
    headers: Map[String, String] = Map.empty): WriterResponse =
    Response(contentType, body, headers)

  /** For usage from Java. Scala users should use the above. */
  def apply(
    contentType: String,
    body: Any,
    headers: java.util.Map[String, String]): WriterResponse =
    Response(contentType, body, headers.asScala.toMap)

  private final case class Response(
    contentType: String = MediaType.OctetStream,
    body: Any,
    headers: Map[String, String] = Map.empty
  ) extends WriterResponse

  private[http] case object EmptyResponse extends WriterResponse {
    override val contentType: String = ""
    override val body: Any = null
    override val headers: Map[String, String] = Map.empty
  }
}

abstract class WriterResponse private() {
  def contentType: String
  def body: Any
  def headers: Map[String, String]
}
