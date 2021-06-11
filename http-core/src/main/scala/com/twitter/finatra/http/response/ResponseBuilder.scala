package com.twitter.finatra.http.response

import com.twitter.finagle
import com.twitter.finagle.http._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http
import com.twitter.finatra.http.marshalling.{MessageBodyFlags, MessageBodyManager}
import com.twitter.finatra.http.streaming.ToReader
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import com.twitter.util.jackson.ScalaObjectMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}
import javax.inject.Inject

object ResponseBuilder {
  private val MediaTypesWithCharsetSupport: Map[String, String] =
    Map(
      MediaType.PlainText -> MediaType.PlainTextUtf8,
      MediaType.Json -> MediaType.JsonUtf8,
      MediaType.Javascript -> MediaType.addUtf8Charset(MediaType.Javascript),
      MediaType.Html -> MediaType.HtmlUtf8,
      MediaType.Xml -> MediaType.XmlUtf8
    )
}

class ResponseBuilder @Inject() (
  objectMapper: ScalaObjectMapper,
  fileResolver: FileResolver,
  messageBodyManager: MessageBodyManager,
  statsReceiver: StatsReceiver,
  @Flag(MessageBodyFlags.ResponseCharsetEnabled) includeContentTypeCharset: Boolean)
    extends Logging {
  import ResponseBuilder._

  // optimized
  private[this] val mimeTypeCache = new ConcurrentHashMap[String, String]()
  private[this] val whenMimeTypeAbsent = new JFunction[String, String] {
    override def apply(mimeType: String): String = {
      if (includeContentTypeCharset && mimeType.indexOf(';') == -1) {
        val mimeTypeWithCharsetOption = MediaTypesWithCharsetSupport.get(mimeType)
        mimeTypeWithCharsetOption.getOrElse(mimeType)
      } else mimeType
    }
  }

  /**
   * Representation of the `text/plain` content type governed by
   * the [[includeContentTypeCharset]] Boolean which determines if the UTF-8 charset encoding
   * parameter should be included in the content type.
   *
   * @see [[com.twitter.finagle.http.MediaType.PlainText]]
   * @see [[com.twitter.finagle.http.MediaType.PlainTextUtf8]]
   * @see [[MessageBodyFlags]]
   */
  val plainTextContentType: String = fullMimeTypeValue(MediaType.PlainText)

  /**
   * Representation of the `application/json` content type governed by
   * the [[includeContentTypeCharset]] Boolean which determines if the UTF-8 charset encoding
   * parameter should be included in the content type.
   *
   * @see [[com.twitter.finagle.http.MediaType.Json]]
   * @see [[com.twitter.finagle.http.MediaType.JsonUtf8]]
   * @see [[MessageBodyFlags]]
   */
  val jsonContentType: String = fullMimeTypeValue(MediaType.Json)

  /**
   * Representation of the `text/html` content type governed by
   * the [[includeContentTypeCharset]] Boolean which determines if the UTF-8 charset encoding
   * parameter should be included in the content type.
   *
   * @see [[com.twitter.finagle.http.MediaType.Html]]
   * @see [[com.twitter.finagle.http.MediaType.HtmlUtf8]]
   * @see [[MessageBodyFlags]]
   */
  val htmlContentType: String = fullMimeTypeValue(MediaType.Html)

  private[this] final val EnrichedResponseBuilder =
    new EnrichedResponse.Builder(
      statsReceiver,
      fileResolver,
      objectMapper,
      messageBodyManager,
      this)

  /* Status Codes */

  /**
   * Returns a response with the given status code.
   * @param statusCode the HTTP status code to set in the returned response
   */
  def status(statusCode: Int): EnrichedResponse = status(Status(statusCode))

  /**
   * Returns a response with the given [[com.twitter.finagle.http.Status]]
   * @param responseStatus the [[com.twitter.finagle.http.Status]] to set in the returned response
   */
  def status(responseStatus: Status): EnrichedResponse = EnrichedResponseBuilder(responseStatus)

  /**
   * Returns an HTTP `200 OK` response.
   */
  def ok: EnrichedResponse = EnrichedResponseBuilder(Status.Ok)

  /**
   * Returns an HTTP `200 OK` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def ok(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Ok).body(body)

  /**
   * Returns an HTTP `200 OK` response with a written body, potentially based on values
   * contained within the [[com.twitter.finagle.http.Request]].
   *
   * @note This version is useful when the `body` parameter requires custom
   *       message body rendering and values in the `Request` are required for
   *       decision making.
   * @param request the HTTP [[com.twitter.finagle.http.Request]] associated with this response
   * @param body the response body, or the information needed to render the body
   */
  def ok(request: finagle.http.Request, body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.Ok).body(request, body)

  /**
   * Returns an HTTP `200 OK` response with a written String body.
   * @param body the response body as a String
   */
  def ok(body: String): EnrichedResponse = EnrichedResponseBuilder(Status.Ok).body(body)

  /**
   * Returns an HTTP `204 No Content` response.
   */
  def noContent: EnrichedResponse = EnrichedResponseBuilder(Status.NoContent)

  /**
   * Returns an HTTP `406 Not Acceptable` response.
   */
  def notAcceptable: EnrichedResponse = EnrichedResponseBuilder(Status.NotAcceptable)

  /**
   * Returns an HTTP `406 Not Acceptable` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def notAcceptable(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.NotAcceptable).body(body)

  /**
   * Returns an HTTP `201 Created` response.
   */
  def created: EnrichedResponse = EnrichedResponseBuilder(Status.Created)

  /**
   * Returns an HTTP `201 Created` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def created(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Created).body(body)

  /**
   * Returns an HTTP `201 Created` response.
   */
  def accepted: EnrichedResponse = EnrichedResponseBuilder(Status.Accepted)

  /**
   * Returns an HTTP `201 Created` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def accepted(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Accepted).body(body)

  /**
   * Returns an HTTP `301 Moved Permanently` response.
   */
  def movedPermanently: EnrichedResponse = EnrichedResponseBuilder(Status.MovedPermanently)

  /**
   * Returns an HTTP `301 Moved Permanently` response with a written body
   * @param body the response body, or the information needed to render the body
   */
  def movedPermanently(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.MovedPermanently).body(body)

  /**
   * Returns an HTTP `302 Found` response.
   */
  def found: EnrichedResponse = EnrichedResponseBuilder(Status.Found)

  /**
   * Returns an HTTP `304 Not Modified` response.
   */
  def notModified: EnrichedResponse = EnrichedResponseBuilder(Status.NotModified)

  /**
   * Returns an HTTP `307 Temporary Redirect` response.
   */
  def temporaryRedirect: EnrichedResponse = EnrichedResponseBuilder(Status.TemporaryRedirect)

  /**
   * Returns an HTTP `405 Method Not Allowed` response.
   */
  def methodNotAllowed: EnrichedResponse = EnrichedResponseBuilder(Status.MethodNotAllowed)

  /**
   * Returns an HTTP `502 Bad Gateway` response.
   */
  def badGateway: EnrichedResponse = EnrichedResponseBuilder(Status.BadGateway)

  /**
   * Returns an HTTP `400 Bad Request` response.
   */
  def badRequest: EnrichedResponse = EnrichedResponseBuilder(Status.BadRequest)

  /**
   * Returns an HTTP `400 Bad Request` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def badRequest(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.BadRequest).body(body)

  /** Returns an HTTP `409 Conflict` response. */
  def conflict: EnrichedResponse = EnrichedResponseBuilder(Status.Conflict)

  /**
   * Returns an HTTP `409 Conflict` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def conflict(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Conflict).body(body)

  /** Returns an HTTP `401 Unauthorized` response. */
  def unauthorized: EnrichedResponse = EnrichedResponseBuilder(Status.Unauthorized)

  /**
   * Returns an HTTP `401 Unauthorized` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def unauthorized(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.Unauthorized).body(body)

  /** Returns an HTTP `403 Forbidden` response */
  def forbidden: EnrichedResponse = EnrichedResponseBuilder(Status.Forbidden)

  /**
   * Returns an HTTP `403 Forbidden` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def forbidden(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Forbidden).body(body)

  /** Returns an HTTP `404 Not Found` response */
  def notFound: EnrichedResponse = EnrichedResponseBuilder(Status.NotFound)

  /**
   * Returns an HTTP `404 Not Found` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def notFound(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.NotFound).body(body)

  /**
   * Returns an HTTP `404 Not Found` response with a written String body.
   * @param body the response body as a String
   */
  def notFound(body: String): EnrichedResponse =
    EnrichedResponseBuilder(Status.NotFound).plain(body)

  /** Returns an HTTP `412 Precondition Failed` response */
  def preconditionFailed: EnrichedResponse = EnrichedResponseBuilder(Status.PreconditionFailed)

  /**
   * Returns an HTTP `412 Precondition Failed` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def preconditionFailed(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.PreconditionFailed).body(body)

  /** Returns an HTTP `413 Request Entity Too Large` response */
  def requestEntityTooLarge: EnrichedResponse =
    EnrichedResponseBuilder(Status.RequestEntityTooLarge)

  /**
   * Returns an HTTP `413 Request Entity Too Large` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def requestEntityTooLarge(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.RequestEntityTooLarge).body(body)

  /** Returns an HTTP `410 Gone` response */
  def gone: EnrichedResponse = EnrichedResponseBuilder(Status.Gone)

  /**
   * Returns an HTTP `410 Gone` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def gone(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Gone).body(body)

  /** Returns an HTTP `500 Internal Server Error` response */
  def internalServerError: EnrichedResponse = EnrichedResponseBuilder(Status.InternalServerError)

  /**
   * Returns an HTTP `500 Internal Server Error` response with a written body.
   * @param body the response body, or the information needed to render the body
   */
  def internalServerError(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.InternalServerError).body(body)

  /** Returns an HTTP `501 Not Implemented` response */
  def notImplemented: EnrichedResponse = EnrichedResponseBuilder(Status.NotImplemented)

  /** Returns an HTTP `503 Service Unavailable` response */
  def serviceUnavailable: EnrichedResponse = EnrichedResponseBuilder(Status.ServiceUnavailable)

  /** Returns an HTTP `499 Client Closed` response */
  def clientClosed: EnrichedResponse = EnrichedResponseBuilder(Status.ClientClosedRequest)

  /**
   * Generic method to wrap a [[com.twitter.finagle.http.Response]] with this builder
   * for augmenting the response.
   * @param response the [[com.twitter.finagle.http.Response]] to wrap.
   */
  def create(response: Response): EnrichedResponse = EnrichedResponseBuilder(response)

  /**
   * Create a StreamingResponse which can be converted to a
   * [[com.twitter.finagle.http.Response]] later.
   *
   * @param stream The output stream.
   * @param status Represents an HTTP status code.
   * @param headers A Map of message headers.
   * @tparam F The Primitive Stream type.
   * @tparam A The type of streaming values.
   */
  def streaming[F[_]: ToReader, A: Manifest](
    stream: F[A],
    status: Status = Status.Ok,
    headers: Map[String, Seq[String]] = Map.empty
  ): http.streaming.StreamingResponse[F, A] =
    new http.streaming.StreamingResponse(objectMapper, stream, status, headers)

  /** Java support for streaming */
  def streaming[F[_]: ToReader, A: Manifest](stream: F[A]): http.streaming.StreamingResponse[F, A] =
    streaming(stream, Status.Ok, Map.empty)

  private[http] def fullMimeTypeValue(mimeType: String): String = {
    mimeTypeCache.computeIfAbsent(mimeType, whenMimeTypeAbsent)
  }
}
