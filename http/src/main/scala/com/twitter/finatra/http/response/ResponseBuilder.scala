package com.twitter.finatra.http.response

import com.twitter.finagle
import com.twitter.finagle.http._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http
import com.twitter.finatra.http.marshalling.{MessageBodyFlags, MessageBodyManager}
import com.twitter.finatra.http.streaming.ToReader
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}
import javax.inject.Inject

object ResponseBuilder {
  val DefaultCharset = "charset=utf-8"
}

class ResponseBuilder @Inject()(
  objectMapper: FinatraObjectMapper,
  fileResolver: FileResolver,
  messageBodyManager: MessageBodyManager,
  statsReceiver: StatsReceiver,
  @Flag(MessageBodyFlags.ResponseCharsetEnabled) includeContentTypeCharset: Boolean)
  extends Logging {

  // optimized
  private[this] val mimeTypeCache = new ConcurrentHashMap[String, String]()
  private[this] val whenMimeTypeAbsent = new JFunction[String, String] {
    override def apply(mimeType: String): String = {
      if (includeContentTypeCharset) {
        if (mimeType.indexOf(';') == -1) mimeType + "; " + ResponseBuilder.DefaultCharset
        else mimeType
      } else
        mimeType
    }
  }

  val plainTextContentType: String = fullMimeTypeValue(MediaType.PlainText)
  val jsonContentType: String = fullMimeTypeValue(MediaType.Json)
  val htmlContentType: String = fullMimeTypeValue(MediaType.Html)

  private[this] val EnrichedResponseBuilder =
    new EnrichedResponseBuilder(
      statsReceiver,
      fileResolver,
      objectMapper,
      messageBodyManager,
      includeContentTypeCharset,
      this)

  /* Status Codes */

  def status(statusCode: Int): EnrichedResponse = status(Status(statusCode))

  def status(responseStatus: Status): EnrichedResponse = EnrichedResponseBuilder(responseStatus)

  def ok: EnrichedResponse = EnrichedResponseBuilder(Status.Ok)

  def ok(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Ok).body(body)

  /**
   * Returns an `Ok` response with a written body, potentially based on values
   * contained within the [[com.twitter.finagle.http.Request]].
   *
   * @note This version is useful when the `body` parameter requires custom
   *       message body rendering and values in the `Request` are required for
   *       decision making.
   * @param request the HTTP Request associated with this response
   * @param body    the response body, or the information needed to render the body
   */
  def ok(request: finagle.http.Request, body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.Ok).body(request, body)

  def ok(body: String): EnrichedResponse = EnrichedResponseBuilder(Status.Ok).body(body)

  def noContent: EnrichedResponse = EnrichedResponseBuilder(Status.NoContent)

  def notAcceptable: EnrichedResponse = EnrichedResponseBuilder(Status.NotAcceptable)

  def notAcceptable(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.NotAcceptable).body(body)

  def created: EnrichedResponse = EnrichedResponseBuilder(Status.Created)

  def created(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Created).body(body)

  def accepted: EnrichedResponse = EnrichedResponseBuilder(Status.Accepted)

  def accepted(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Accepted).body(body)

  def movedPermanently: EnrichedResponse = EnrichedResponseBuilder(Status.MovedPermanently)

  def movedPermanently(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.MovedPermanently).body(body)

  def found: EnrichedResponse = EnrichedResponseBuilder(Status.Found)

  def notModified: EnrichedResponse = EnrichedResponseBuilder(Status.NotModified)

  def temporaryRedirect: EnrichedResponse = EnrichedResponseBuilder(Status.TemporaryRedirect)

  def methodNotAllowed: EnrichedResponse = EnrichedResponseBuilder(Status.MethodNotAllowed)

  def badGateway: EnrichedResponse = EnrichedResponseBuilder(Status.BadGateway)

  def badRequest: EnrichedResponse = EnrichedResponseBuilder(Status.BadRequest)

  def badRequest(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.BadRequest).body(body)

  def conflict: EnrichedResponse = EnrichedResponseBuilder(Status.Conflict)

  def conflict(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Conflict).body(body)

  def unauthorized: EnrichedResponse = EnrichedResponseBuilder(Status.Unauthorized)

  def unauthorized(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.Unauthorized).body(body)

  def forbidden: EnrichedResponse = EnrichedResponseBuilder(Status.Forbidden)

  def forbidden(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Forbidden).body(body)

  def notFound: EnrichedResponse = EnrichedResponseBuilder(Status.NotFound)

  def notFound(body: String): EnrichedResponse =
    EnrichedResponseBuilder(Status.NotFound).plain(body)

  def notFound(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.NotFound).body(body)

  def preconditionFailed: EnrichedResponse = EnrichedResponseBuilder(Status.PreconditionFailed)

  def preconditionFailed(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.PreconditionFailed).body(body)

  def requestEntityTooLarge: EnrichedResponse =
    EnrichedResponseBuilder(Status.RequestEntityTooLarge)

  def requestEntityTooLarge(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.RequestEntityTooLarge).body(body)

  def gone: EnrichedResponse = EnrichedResponseBuilder(Status.Gone)

  def gone(body: Any): EnrichedResponse = EnrichedResponseBuilder(Status.Gone).body(body)

  def internalServerError: EnrichedResponse = EnrichedResponseBuilder(Status.InternalServerError)

  def internalServerError(body: Any): EnrichedResponse =
    EnrichedResponseBuilder(Status.InternalServerError).body(body)

  def notImplemented: EnrichedResponse = EnrichedResponseBuilder(Status.NotImplemented)

  def serviceUnavailable: EnrichedResponse = EnrichedResponseBuilder(Status.ServiceUnavailable)

  def clientClosed: EnrichedResponse = EnrichedResponseBuilder(Status.ClientClosedRequest)

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
