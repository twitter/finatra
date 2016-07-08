package com.twitter.finatra.http.response

import com.google.common.net.{HttpHeaders, MediaType}
import com.twitter.finagle.http.{Cookie => FinagleCookie, _}
import com.twitter.finagle.netty3.ChannelBufferBuf
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.exceptions.HttpResponseException
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.http.routing.FileResolver
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.inject.exceptions.DetailedNonRetryableSourcedException
import com.twitter.io.Buf
import com.twitter.util.{Future, Memoize}
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import javax.inject.Inject
import org.apache.commons.io.FilenameUtils._
import org.apache.commons.io.IOUtils
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.handler.codec.http.{Cookie => NettyCookie, DefaultCookie}
import scala.runtime.BoxedUnit

object ResponseBuilder {
  val DefaultCharset = "charset=utf-8"
  val PlainTextContentType = "text/plain; " + DefaultCharset
}

class ResponseBuilder @Inject()(
  objectMapper: FinatraObjectMapper,
  fileResolver: FileResolver,
  messageBodyManager: MessageBodyManager,
  statsReceiver: StatsReceiver)
  extends Logging {

  // generates stats in the form: service/failure/[source]/[details]
  private val serviceFailureNamespace = Seq("service", "failure")
  private val serviceFailureScoped = statsReceiver.scope(serviceFailureNamespace: _*)
  private val serviceFailureCounter = statsReceiver.counter(serviceFailureNamespace: _*)

  /* Status Codes */

  def status(statusCode: Int): EnrichedResponse = status(Status(statusCode))

  def status(responseStatus: Status): EnrichedResponse = EnrichedResponse(responseStatus)

  def ok: EnrichedResponse = EnrichedResponse(Status.Ok)

  def ok(body: Any): EnrichedResponse = EnrichedResponse(Status.Ok).body(body)

  /**
   * Returns an `Ok` response with a written body, potentially based on values
   * contained within the `Request`.
   *
   * @note This version is useful when the `body` parameter requires custom
   *       message body rendering and values in the `Request` are required for
   *       decision making.
   * @param request the HTTP Request associated with this response
   * @param body    the response body, or the information needed to render the body
   */
  def ok(request: Request, body: Any): EnrichedResponse = EnrichedResponse(Status.Ok).body(request, body)

  def ok(body: String): EnrichedResponse = EnrichedResponse(Status.Ok).body(body)

  def noContent: EnrichedResponse = EnrichedResponse(Status.NoContent)

  def notAcceptable: EnrichedResponse = EnrichedResponse(Status.NotAcceptable)

  def notAcceptable(body: Any): EnrichedResponse = EnrichedResponse(Status.NotAcceptable).body(body)

  def created = EnrichedResponse(Status.Created)

  def created(body: Any) = EnrichedResponse(Status.Created).body(body)

  def accepted = EnrichedResponse(Status.Accepted)

  def accepted(body: Any) = EnrichedResponse(Status.Accepted).body(body)

  def movedPermanently = EnrichedResponse(Status.MovedPermanently)

  def movedPermanently(body: Any) = EnrichedResponse(Status.MovedPermanently).body(body)

  def found = EnrichedResponse(Status.Found)

  def notModified = EnrichedResponse(Status.NotModified)

  def temporaryRedirect = EnrichedResponse(Status.TemporaryRedirect)

  def methodNotAllowed = EnrichedResponse(Status.MethodNotAllowed)

  def badGateway = EnrichedResponse(Status.BadGateway)

  def badRequest = EnrichedResponse(Status.BadRequest)

  def badRequest(body: Any) = EnrichedResponse(Status.BadRequest).body(body)

  def conflict = EnrichedResponse(Status.Conflict)

  def conflict(body: Any) = EnrichedResponse(Status.Conflict).body(body)

  def unauthorized = EnrichedResponse(Status.Unauthorized)

  def unauthorized(body: Any) = EnrichedResponse(Status.Unauthorized).body(body)

  def forbidden = EnrichedResponse(Status.Forbidden)

  def forbidden(body: Any) = EnrichedResponse(Status.Forbidden).body(body)

  def notFound = EnrichedResponse(Status.NotFound)

  def notFound(body: String) = EnrichedResponse(Status.NotFound).plain(body)

  def notFound(body: Any) = EnrichedResponse(Status.NotFound).body(body)

  def preconditionFailed = EnrichedResponse(Status.PreconditionFailed)

  def preconditionFailed(body: Any) = EnrichedResponse(Status.PreconditionFailed).body(body)

  def requestEntityTooLarge = EnrichedResponse(Status.RequestEntityTooLarge)

  def requestEntityTooLarge(body: Any) = EnrichedResponse(Status.RequestEntityTooLarge).body(body)

  def gone = EnrichedResponse(Status.Gone)

  def gone(body: Any) = EnrichedResponse(Status.Gone).body(body)

  def internalServerError = EnrichedResponse(Status.InternalServerError)

  def internalServerError(body: Any) = EnrichedResponse(Status.InternalServerError).body(body)

  def notImplemented = EnrichedResponse(Status.NotImplemented)

  def serviceUnavailable = EnrichedResponse(Status.ServiceUnavailable)

  def clientClosed = EnrichedResponse(Status.ClientClosedRequest)

  def create(response: Response) = new EnrichedResponse(response)

  object EnrichedResponse {
    def apply(s: Status): EnrichedResponse = EnrichedResponse(Response(Version.Http11, s))
  }

  /* Wrapper around Finagle Response which exposes a builder-like API */
  case class EnrichedResponse(resp: Response)
    extends ResponseProxy {
    override val response = resp

    /* Public */

    def cookie(k: String, v: String): EnrichedResponse = {
      cookie(new FinagleCookie(new DefaultCookie(k, v)))
      this
    }

    def cookie(c: FinagleCookie): EnrichedResponse = {
      response.addCookie(c)
      this
    }

    def cookie(c: NettyCookie): EnrichedResponse = {
      response.addCookie(new FinagleCookie(c))
      this
    }

    def json(obj: Any) = {
      contentTypeJson()

      obj match {
        case bytes: Array[Byte] => body(bytes)
        case str: String => body(str)
        case _ =>
          response.withOutputStream { os =>
            objectMapper.writeValue(obj, os)
          }
      }
      this
    }

    def jsonError: EnrichedResponse = {
      jsonError(status.reason.toLowerCase)
      this
    }

    def jsonError(message: String): EnrichedResponse = {
      json(ErrorsResponse(message))
      this
    }

    def body(any: Any): EnrichedResponse = body(None, any)

    /**
     * Returns a response with a written body, potentially based on values
     * contained within the `Request`.
     *
     * @note This version is useful when the `any` parameter requires custom
     *       message body rendering and values in the `Request` are required for
     *       decision making.
     * @param request the HTTP Request associated with this response
     * @param any     the body, or the information needed to render the body
     */
    def body(request: Request, any: Any): EnrichedResponse = body(Some(request), any)

    def file(file: File): EnrichedResponse = {
      body(
        new BufferedInputStream(
          new FileInputStream(file)))

      contentType(
        fileResolver.getContentType(file.getName))
    }

    def body(b: Array[Byte]): EnrichedResponse = {
      response.content = Buf.ByteArray.Owned(b)
      this
    }

    def body(bodyStr: String): EnrichedResponse = {
      response.setContentString(bodyStr)
      this
    }

    def body(inputStream: InputStream): EnrichedResponse = {
      body(
        ChannelBufferBuf.Owned(
          ChannelBuffers.wrappedBuffer(
            IOUtils.toByteArray(inputStream)
          )
        )
      )
      this
    }

    @deprecated("use body(Buf)", "2015-08-20")
    def body(channelBuffer: ChannelBuffer): EnrichedResponse = {
      response.content = ChannelBufferBuf.Owned(channelBuffer)
      this
    }

    def body(buffer: Buf): EnrichedResponse = {
      response.content = buffer
      this
    }

    def contentTypeJson() = {
      contentType("application/json")
      this
    }

    def nothing = {
      this
    }

    def plain(any: Any): EnrichedResponse = {
      response.headerMap.set(HttpHeaders.CONTENT_TYPE, mediaToString(MediaType.PLAIN_TEXT_UTF_8))
      body(any)
    }

    def html(html: String) = {
      response.headerMap.set(HttpHeaders.CONTENT_TYPE, mediaToString(MediaType.HTML_UTF_8))
      body(html)
      this
    }

    def html(any: Any) = {
      response.headerMap.set(HttpHeaders.CONTENT_TYPE, mediaToString(MediaType.HTML_UTF_8))
      body(any)
      this
    }

    def location(uri: Any): EnrichedResponse = {
      location(uri.toString)
    }

    def location(uri: String): EnrichedResponse = {
      response.headerMap.set("Location", uri)
      this
    }

    def header(k: String, v: Any) = {
      response.headerMap.set(k, v.toString)
      this
    }

    def header(k: String, v: MediaType) = {
      response.headerMap.set(k, mediaToString(v))
      this
    }

    def headers(map: Map[String, String]) = {
      for ((k, v) <- map) {
        response.headerMap.set(k, v)
      }
      this
    }

    def headers(entries: (String, Any)*) = {
      for ((k, v) <- entries) {
        response.headerMap.set(k, v.toString)
      }
      this
    }

    def contentType(mimeType: String) = {
      response.headerMap.set(
        Fields.ContentType,
        mimeType + "; " + ResponseBuilder.DefaultCharset)
      this
    }

    def contentType(mimeType: MediaType) = {
      response.headerMap.set(
        Fields.ContentType,
        mediaToString(mimeType))
      this
    }

    def file(file: String): Response = {
      val fileWithSlash = if (file.startsWith("/")) file else "/" + file
      fileResolver.getInputStream(fileWithSlash) map { inputStream =>
        contentType(fileResolver.getContentType(file))
        body(inputStream)
      } getOrElse {
        notFound.plain(fileWithSlash + " not found")
      }
    }

    /**
     * Return the file (only if it's a file w/ an extension), otherwise return the index.
     * Note: This functionality is useful for "single-page" UI frameworks (e.g. AngularJS)
     * that perform client side routing.
     */
    def fileOrIndex(filePath: String, indexPath: String) = {
      if (isFile(filePath))
        file(filePath)
      else
        file(indexPath)
    }

    def view(template: String, obj: Any) = {
      html(MustacheBodyComponent(obj, template))
    }

    /* Exception Stats */

    /**
     * Helper method for returning responses that are the result of a "service-level" failure. This is most commonly
     * useful in an [[com.twitter.finatra.http.exceptions.ExceptionMapper]] implementation. E.g.,
     *
     * @\Singleton
     * class AuthenticationExceptionMapper @Inject()(
     *   response: ResponseBuilder)
     * extends ExceptionMapper[AuthenticationException] {
     *
     *   override def toResponse(
     *     request: Request,
     *     exception: AuthenticationException
     *   ): Response = {
     *     response
     *       .status(exception.status)
     *       .failureClassifier(
     *         is5xxStatus(exception.status),
     *         request,
     *         exception)
     *       .jsonError( s"Your account could not be authenticated. Reason: ${exception.resultCode}")
     *   }
     * }
     * @param request - the [[com.twitter.finagle.http.Request]] that triggered the failure
     * @param source - The named component responsible for causing this failure.
     * @param details - Details about this exception suitable for stats. Each element will be converted into a string.
     *                Note: Each element must have a bounded set of values (e.g. You can stat the type of a tweet
     *                as "protected" or "unprotected", but you can't include the actual tweet id "696081566032723968").
     * @param message - Details about this exception to be logged when this exception occurs. Typically logDetails
     *                contains the unbounded details of the exception that you are not able to stat such as an
     *                actual tweet ID (see above)
     *
     * @see [[EnrichedResponse#failureClassifier]]
     * @return this [[EnrichedResponse]]
     */
    def failure(
      request: Request,
      source: String,
      details: Seq[Any],
      message: String = ""
    ): ResponseBuilder#EnrichedResponse = {
      failureClassifier(classifier = true, request, source, details, message)
    }

    def failure(
      request: Request,
      exception: DetailedNonRetryableSourcedException
    ): ResponseBuilder#EnrichedResponse = {
      failureClassifier(classifier = true, request, exception.source, exception.details, exception.message)
    }

    def failureClassifier(
      classifier: => Boolean,
      request: Request,
      exception: DetailedNonRetryableSourcedException
    ): ResponseBuilder#EnrichedResponse = {
      failureClassifier(classifier, request, exception.source, exception.details)
    }

    /**
     * Helper method for returning responses that are the result of a "service-level" failure. This is most commonly
     * useful in an [[com.twitter.finatra.http.exceptions.ExceptionMapper]] implementation.
     *
     * @param classifier - if the failure should be "classified", e.g. logged and stat'd accordingly
     * @param request - the [[com.twitter.finagle.http.Request]] that triggered the failure
     * @param source - The named component responsible for causing this failure.
     * @param details - Details about this exception suitable for stats. Each element will be converted into a string.
     *                Note: Each element must have a bounded set of values (e.g. You can stat the type of a tweet
     *                as "protected" or "unprotected", but you can't include the actual tweet id "696081566032723968").
     * @param message - Details about this exception to be logged when this exception occurs. Typically logDetails
     *                   contains the unbounded details of the exception that you are not able to stat such as an
     *                   actual tweet ID (see above)
     * @return this [[EnrichedResponse]]
     */
    def failureClassifier(
      classifier: => Boolean,
      request: Request,
      source: String,
      details: Seq[Any] = Seq(),
      message: String = ""
    ): ResponseBuilder#EnrichedResponse = {
      if (classifier) {
        val detailStrings = details.map(_.toString)
        warn(s"Request Failure: $source/" + detailStrings.mkString("/") + " " + message)
        serviceFailureCounter.incr() // service/failure
        serviceFailureScoped.counter(source).incr() // service/failure/AuthService
        serviceFailureScoped.scope(source).counter(detailStrings: _*).incr() // service/failure/AuthService/3040/Bad_signature
        routeScopedFailure(request).scope(source).counter(detailStrings: _*).incr() // route/hello/POST/failure/AuthService/3040/Bad_signature
      }

      this
    }

    /* Public Conversions */

    def toFuture: Future[Response] = Future.value(response)

    def toException: HttpResponseException = new HttpResponseException(response)

    def toFutureException[T]: Future[T] = Future.exception(toException)

    /* Private */

    private def isFile(requestPath: String) = {
      getExtension(requestPath).nonEmpty
    }

    //optimized: MediaType.toString is a hotspot when profiling
    private val mediaToString = Memoize { mediaType: MediaType =>
      mediaType.toString
    }

    private def routeScopedFailure(request: Request): StatsReceiver = {
      val routeInfo = RouteInfo(request).getOrElse(throw new Exception("routeScopedFailure can only be used within a HTTP request callback"))
      statsReceiver.scope("route", routeInfo.sanitizedPath, request.method.toString(), "failure")
    }

    private def body(request: Option[Request], any: Any): EnrichedResponse = {
      any match {
        case null => nothing
        case buf: Buf => body(buf)
        case bytes: Array[Byte] => body(bytes)
        case cbos: ChannelBuffer => body(ChannelBufferBuf.Owned(cbos))
        case "" => nothing
        case Unit => nothing
        case _: BoxedUnit => nothing
        case opt if opt == None => nothing
        case str: String => body(str)
        case _file: File => file(_file)
        case _ =>
          val writer = messageBodyManager.writer(any)
          val writerResponse = request match {
            case Some(req) => writer.write(req, any)
            case None => writer.write(any)
          }
          body(writerResponse.body)
          contentType(writerResponse.contentType)
          headers(writerResponse.headers)
      }
      this
    }
  }
}
