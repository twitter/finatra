package com.twitter.finatra.http.response

import com.twitter.finagle
import com.twitter.finagle.http._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.exceptions.HttpResponseException
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.http.streaming.ToReader
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.AutoClosable.tryWith
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import com.twitter.inject.exceptions.DetailedNonRetryableSourcedException
import com.twitter.io.{Buf, StreamIO}
import com.twitter.util.Future
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}
import javax.inject.Inject
import scala.runtime.BoxedUnit

object ResponseBuilder {
  val DefaultCharset = "charset=utf-8"
}

class ResponseBuilder @Inject()(
  objectMapper: FinatraObjectMapper,
  fileResolver: FileResolver,
  messageBodyManager: MessageBodyManager,
  statsReceiver: StatsReceiver,
  @Flag("http.response.charset.enabled") includeContentTypeCharset: Boolean
) extends Logging {

  // generates stats in the form: service/failure/[source]/[details]
  private val serviceFailureNamespace = Seq("service", "failure")
  private val serviceFailureScoped = statsReceiver.scope(serviceFailureNamespace: _*)
  private val serviceFailureCounter = statsReceiver.counter(serviceFailureNamespace: _*)

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

  /* Status Codes */

  def status(statusCode: Int): EnrichedResponse = status(Status(statusCode))

  def status(responseStatus: Status): EnrichedResponse = EnrichedResponse(responseStatus)

  def ok: EnrichedResponse = EnrichedResponse(Status.Ok)

  def ok(body: Any): EnrichedResponse = EnrichedResponse(Status.Ok).body(body)

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
    EnrichedResponse(Status.Ok).body(request, body)

  def ok(body: String): EnrichedResponse = EnrichedResponse(Status.Ok).body(body)

  def noContent: EnrichedResponse = EnrichedResponse(Status.NoContent)

  def notAcceptable: EnrichedResponse = EnrichedResponse(Status.NotAcceptable)

  def notAcceptable(body: Any): EnrichedResponse = EnrichedResponse(Status.NotAcceptable).body(body)

  def created: EnrichedResponse = EnrichedResponse(Status.Created)

  def created(body: Any): EnrichedResponse = EnrichedResponse(Status.Created).body(body)

  def accepted: EnrichedResponse = EnrichedResponse(Status.Accepted)

  def accepted(body: Any): EnrichedResponse = EnrichedResponse(Status.Accepted).body(body)

  def movedPermanently: EnrichedResponse = EnrichedResponse(Status.MovedPermanently)

  def movedPermanently(body: Any): EnrichedResponse =
    EnrichedResponse(Status.MovedPermanently).body(body)

  def found: EnrichedResponse = EnrichedResponse(Status.Found)

  def notModified: EnrichedResponse = EnrichedResponse(Status.NotModified)

  def temporaryRedirect: EnrichedResponse = EnrichedResponse(Status.TemporaryRedirect)

  def methodNotAllowed: EnrichedResponse = EnrichedResponse(Status.MethodNotAllowed)

  def badGateway: EnrichedResponse = EnrichedResponse(Status.BadGateway)

  def badRequest: EnrichedResponse = EnrichedResponse(Status.BadRequest)

  def badRequest(body: Any): EnrichedResponse = EnrichedResponse(Status.BadRequest).body(body)

  def conflict: EnrichedResponse = EnrichedResponse(Status.Conflict)

  def conflict(body: Any): EnrichedResponse = EnrichedResponse(Status.Conflict).body(body)

  def unauthorized: EnrichedResponse = EnrichedResponse(Status.Unauthorized)

  def unauthorized(body: Any): EnrichedResponse = EnrichedResponse(Status.Unauthorized).body(body)

  def forbidden: EnrichedResponse = EnrichedResponse(Status.Forbidden)

  def forbidden(body: Any): EnrichedResponse = EnrichedResponse(Status.Forbidden).body(body)

  def notFound: EnrichedResponse = EnrichedResponse(Status.NotFound)

  def notFound(body: String): EnrichedResponse = EnrichedResponse(Status.NotFound).plain(body)

  def notFound(body: Any): EnrichedResponse = EnrichedResponse(Status.NotFound).body(body)

  def preconditionFailed: EnrichedResponse = EnrichedResponse(Status.PreconditionFailed)

  def preconditionFailed(body: Any): EnrichedResponse =
    EnrichedResponse(Status.PreconditionFailed).body(body)

  def requestEntityTooLarge: EnrichedResponse = EnrichedResponse(Status.RequestEntityTooLarge)

  def requestEntityTooLarge(body: Any): EnrichedResponse =
    EnrichedResponse(Status.RequestEntityTooLarge).body(body)

  def gone: EnrichedResponse = EnrichedResponse(Status.Gone)

  def gone(body: Any): EnrichedResponse = EnrichedResponse(Status.Gone).body(body)

  def internalServerError: EnrichedResponse = EnrichedResponse(Status.InternalServerError)

  def internalServerError(body: Any): EnrichedResponse =
    EnrichedResponse(Status.InternalServerError).body(body)

  def notImplemented: EnrichedResponse = EnrichedResponse(Status.NotImplemented)

  def serviceUnavailable: EnrichedResponse = EnrichedResponse(Status.ServiceUnavailable)

  def clientClosed: EnrichedResponse = EnrichedResponse(Status.ClientClosedRequest)

  def create(response: Response): EnrichedResponse = new EnrichedResponse(response)

  /**
   * Experimental, create a StreamingResponse which can be converted to a
   * [[com.twitter.finagle.http.Response]] later.
   *
   * @param stream The output stream.
   * @tparam F The Primitive Stream type.
   * @tparam A The type of streaming values.
   */
  private[finatra] def streaming[F[_]: ToReader, A](stream: F[A]): http.streaming.StreamingResponse[F, A] =
    http.streaming.StreamingResponse(objectMapper, stream)

  private def fullMimeTypeValue(mimeType: String): String = {
    mimeTypeCache.computeIfAbsent(mimeType, whenMimeTypeAbsent)
  }

  object EnrichedResponse {
    def apply(s: Status): EnrichedResponse = EnrichedResponse(Response(Version.Http11, s))
  }

  /**
   * A wrapper around a [[com.twitter.finagle.http.Response]] which exposes a
   * builder-like API for creating responses.
   *
   * @param underlying the underlying [[com.twitter.finagle.http.Response]]
   * @see [[com.twitter.finagle.http.Response]]
   */
  case class EnrichedResponse(underlying: finagle.http.Response) extends ResponseProxy {
    override val response = underlying

    /* Public */

    /**
     * Set a [[com.twitter.finagle.http.Cookie]] with the given name and value on
     * the returned Response.
     *
     * @param name [[com.twitter.finagle.http.Cookie]] name
     * @param value [[com.twitter.finagle.http.Cookie]] value
     * @return an [[EnrichedResponse]] with a created [[com.twitter.finagle.http.Cookie]] of name with value.
     * @see [[https://en.wikipedia.org/wiki/HTTP_cookie#Structure HTTP Cookie Structure]]
     */
    def cookie(name: String, value: String): EnrichedResponse = {
      cookie(new Cookie(name, value))
      this
    }

    /**
     * Set the given [[com.twitter.finagle.http.Cookie]] on the returned Response.
     *
     * @param c the [[com.twitter.finagle.http.Cookie]] to set.
     * @return an [[EnrichedResponse]] with a created Cookie of name with value.
     * @see [[https://en.wikipedia.org/wiki/HTTP_cookie HTTP Cookie]]
     */
    def cookie(c: Cookie): EnrichedResponse = {
      response.addCookie(c)
      this
    }

    /**
     * Return a JSON-formatted body from the given object. Note when either String or Array[Byte]
     * data is passed the data is set as the response body unmodified, i.e., assumes the data is
     * already JSON formatted).
     *
     * The purpose of this method is to convert an object into a JSON-formatted body via the
     * defined ObjectMapper.
     *
     * Sets the response "Content-Type" header to "application/json"
     *
     * @param obj object to convert into a JSON-formatted body.
     * @return an [[EnrichedResponse]] with a JSON body.
     */
    def json(obj: Any): EnrichedResponse = {
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

    /**
     * Create a JSON-formatted [[ErrorsResponse]] response body based on
     * this response's current [[com.twitter.finagle.http.Status]].
     *
     * @return an [[EnrichedResponse]] with a JSON-formatted [[ErrorsResponse]] response body.
     * @see [[ErrorsResponse]]
     * @see [[com.twitter.finagle.http.Status]]
     */
    def jsonError: EnrichedResponse = {
      jsonError(status.reason.toLowerCase)
      this
    }

    /**
     * Create a JSON-formatted [[ErrorsResponse]] response body based on the given string.
     *
     * @param message string message over which to create a JSON-formatted [[ErrorsResponse]].
     * @return an [[EnrichedResponse]] with a JSON-formatted [[ErrorsResponse]] response body.
     * @see [[ErrorsResponse]]
     */
    def jsonError(message: String): EnrichedResponse = {
      json(ErrorsResponse(message))
      this
    }

    /**
     * Return a response with the given object as the written response body. The object is
     * pattern-matched to set the body and content type appropriately.
     *
     * @param any the body, or the information needed to render the body.
     * @return an [[EnrichedResponse]] with the given object written as the response body.
     */
    def body(any: Any): EnrichedResponse = body(None, any)

    /**
     * Return a response with a written body potentially based on values
     * contained within the [[com.twitter.finagle.http.Request]].
     *
     * @note This version is useful when the `any` parameter requires custom
     *       message body rendering and values in the `Request` are required for
     *       decision making.
     * @param request the [[com.twitter.finagle.http.Request]] associated with this response.
     * @param any     the body, or the information needed to render the body.
     * @return an [[EnrichedResponse]] with the given object written as the response body.
     */
    def body(request: finagle.http.Request, any: Any): EnrichedResponse = body(Some(request), any)

    /**
     * Return a response with the given Array[Byte] written as the response body.
     *
     * @note this uses [[com.twitter.io.Buf.ByteArray.Owned]] to set the response content.
     *
     * @param b bytes to write as the response body.
     * @return an [[EnrichedResponse]] with the given Array[Byte] written as the response body.
     * @see [[com.twitter.io.Buf.ByteArray.Owned]]
     */
    def body(b: Array[Byte]): EnrichedResponse = {
      response.content = Buf.ByteArray.Owned(b)
      this
    }

    /**
     * Return a response with the given String written as the response body.
     *
     * @param bodyStr the String to write as the response body.
     * @return  an [[EnrichedResponse]] with the given String written as the response body.
     */
    def body(bodyStr: String): EnrichedResponse = {
      response.setContentString(bodyStr)
      this
    }

    /**
     * Return a response with the given InputStream written as bytes as the response body.
     *
     * @note this fully buffers the InputStream as an Array[Byte] and copies it as the
     *       response body. The given input stream is closed after writing the response body.
     * @param inputStream the InputStream to write as bytes as the response body.
     * @return an [[EnrichedResponse]] with the given InputStream written as an Array[Byte] as the
     *         response body.
     */
    def body(inputStream: InputStream): EnrichedResponse = {
      tryWith(inputStream) { closable =>
        body(StreamIO.buffer(closable).toByteArray)
      }
      this
    }

    /**
     * Return a response with the given [[com.twitter.io.Buf]] set as the response body.
     *
     * @param buffer the [[com.twitter.io.Buf]] to set as the response body.
     * @return an [[EnrichedResponse]] with the given [[com.twitter.io.Buf]] set as the response body.
     */
    def body(buffer: Buf): EnrichedResponse = {
      response.content = buffer
      this
    }

    /**
     * Set the "Content-Type" header field for this [[EnrichedResponse]] to "application/json".
     * @return an [[EnrichedResponse]] with a the response header "Content-Type" field set
     *         to "application/json".
     */
    def contentTypeJson(): EnrichedResponse = {
      response.headerMap.set(Fields.ContentType, jsonContentType)
      this
    }

    /**
     * Return the currently built [[EnrichedResponse]] as-is.
     * @return an [[EnrichedResponse]].
     */
    def nothing: EnrichedResponse = {
      this
    }

    /**
     * Return a response with the given object written as a plaintext response body. Sets
     * the "Content-Type" header field for this [[EnrichedResponse]] to "text/plain".
     *
     * @param any the object to write as a plaintext response body.
     * @return an [[EnrichedResponse]] with the given object written as a plaintext response body.
     */
    def plain(any: Any): EnrichedResponse = {
      response.headerMap.set(Fields.ContentType, plainTextContentType)
      body(any)
    }

    /**
     * Return a response with the given String written as a HTML response body. Sets
     * the "Content-Type" header field for this [[EnrichedResponse]] to "text/html".
     *
     * @param html the String to write as an HTML response body.
     * @return an [[EnrichedResponse]] with the given String written as an HTML response body.
     */
    def html(html: String): EnrichedResponse = {
      response.headerMap.set(Fields.ContentType, htmlContentType)
      body(html)
      this
    }

    /**
     * Return a response with the given object written as a HTML response body. Sets
     * the "Content-Type" header field for this [[EnrichedResponse]] to "text/html". The
     * object is pattern-matched to set the response body appropriately.
     *
     * @note the content type can be overridden depending on if there is a registered
     *       [[MessageBodyManager]] for the given object type that specifies a different
     *       content type than "text/html".
     *
     * @param any the object to write as an HTML response body.
     * @return an [[EnrichedResponse]] with the given object written as an HTML response body.
     */
    def html(any: Any): EnrichedResponse = {
      response.headerMap.set(Fields.ContentType, htmlContentType)
      body(any)
      this
    }

    /**
     * Return a response with the "Location" response header field set to the given URI value.
     *
     * @param uri the value to set as the header value. This method calls `toString` on
     *            this parameter to compute the String value.
     * @return an [[EnrichedResponse]] with a set "Location" response header.
     */
    def location(uri: Any): EnrichedResponse = {
      location(uri.toString)
    }

    /**
     * Return a response with the "Location" response header field set to the given URI value.
     *
     * @param uri the String value to set as the header value.
     * @return an [[EnrichedResponse]] with a set "Location" response header.
     */
    def location(uri: String): EnrichedResponse = {
      response.headerMap.set(Fields.Location, uri)
      this
    }

    /**
     * Return a response with the given response header key/value set.
     * @param k the response header key to set.
     * @param v the response header object to set. This method calls `toString` on this parameter
     *         to compute the String value.
     * @return an [[EnrichedResponse]] with the given response header field to the given value.
     */
    def header(k: String, v: Any): EnrichedResponse = {
      response.headerMap.set(k, v.toString)
      this
    }

    /**
     * Return a response with the given response headers set.
     *
     * @param map the map of response header key/values to set.
     * @return an [[EnrichedResponse]] with each response header field set to its mapped value.
     */
    def headers(map: Map[String, String]): EnrichedResponse = {
      for ((k, v) <- map) {
        response.headerMap.set(k, v)
      }
      this
    }

    /**
     * Return a response with the given response headers set.
     *
     * @param entries the sequence of Tuple2 response header key/values to set.
     * @return an [[EnrichedResponse]] with each response header field set to its tupled value.
     */
    def headers(entries: (String, Any)*): EnrichedResponse = {
      for ((k, v) <- entries) {
        response.headerMap.set(k, v.toString)
      }
      this
    }

    /**
     * Set the "Content-Type" response header field of this [[EnrichedResponse]] to the
     * given String.
     *
     * @param mediaType the String to set a the header value.
     * @return an [[EnrichedResponse]] with the "Content-Type" response header field set to given media type.
     */
    def contentType(mediaType: String): EnrichedResponse = {
      response.headerMap.set(Fields.ContentType, fullMimeTypeValue(mediaType))
      this
    }

    /**
     * Return a response with the given [[java.io.File]] bytes as the response body. The file
     * contents are fully buffered and copied to an Array[Byte] before being written as the response
     * body.
     *
     * @note The given [[java.io.File]] is closed after writing the response body.
     * @param file the [[java.io.File]] to write as the response body.
     * @return an [[EnrichedResponse]] with the given file written as an Array[Byte] as the response body.
     */
    def file(file: File): EnrichedResponse = {
      tryWith(new BufferedInputStream(new FileInputStream(file))) { closable =>
        contentType(fileResolver.getContentType(file.getName))
        body(closable)
      }
    }

    /**
     * Return a response resolving the given file path to a [[java.io.File]] using the configured
     * [[com.twitter.finatra.utils.FileResolver]]. The resolved [[java.io.File]] is set as bytes as
     * the response body. The file contents are fully buffered and copied to an Array[Byte] before being
     * written as the response body.
     *
     * @note The resolved file is closed after writing the response body.
     * @param file the file String to resolve as a [[java.io.File]] to write as the response body.
     * @return an [[EnrichedResponse]] with the given file written as an Array[Byte] as the response body.
     * @see [[com.twitter.finatra.utils.FileResolver]]
     */
    def file(file: String): EnrichedResponse = {
      val fileWithSlash = if (file.startsWith("/")) file else "/" + file
      fileResolver
        .getInputStream(fileWithSlash).map { is =>
          contentType(fileResolver.getContentType(file))
          body(is)
        }.getOrElse(notFound.plain(fileWithSlash + " not found"))
    }

    /**
     * Return the file (only if it's an existing file w/ an extension), otherwise return the index.
     * Note: This functionality is useful for "single-page" UI frameworks (e.g. AngularJS)
     * that perform client side routing.
     *
     * @param filePath the file path to resolve and return
     * @param indexPath the index path to use when the file at the given filePath does not exist or
     *                  does not specify an extension.
     * @return an [[EnrichedResponse]] with the resolved file or index written as an Array[Byte] as the
     *         response body.
     * @see [[EnrichedResponse.file(file: String)]]
     */
    def fileOrIndex(filePath: String, indexPath: String): EnrichedResponse = {
      if (exists(filePath) && hasExtension(filePath)) {
        file(filePath)
      } else {
        file(indexPath)
      }
    }

    /**
     * Return a response with the given Mustache template and object data rendered and written as
     * the response body.
     *
     * @param template the template name to render.
     * @param obj the data to render.
     * @return an [[EnrichedResponse]] with the given Mustache template object written as the
     *         response body.
     * @see [[com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent]]
     */
    def view(template: String, obj: Any): EnrichedResponse = {
      body(MustacheBodyComponent(obj, template, getContentType.getOrElse(MediaType.Html)))
      this
    }

    /**
     * Return a response with the given Mustache object data rendered and written as
     * the response body.
     *
     * @param obj object data to render.
     * @return an [[EnrichedResponse]] with the given Mustache template object written as the
     *         response body.
     * @see [[com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent]]
     */
    def view(obj: Any): EnrichedResponse = {
      view("", obj)
    }

    /* Exception Stats */

    /**
     * Helper method for returning responses that are the result of a "service-level" failure. This is most commonly
     * useful in an [[com.twitter.finatra.http.exceptions.ExceptionMapper]] implementation. E.g.,
     *
     * @\Singleton
     * class AuthenticationExceptionMapper @Inject()(
     *  response: ResponseBuilder)
     * extends ExceptionMapper[AuthenticationException] {
     *
     *  override def toResponse(
     *    request: Request,
     *    exception: AuthenticationException
     *  ): Response = {
     *    response
     *      .status(exception.status)
     *      .failureClassifier(
     *        is5xxStatus(exception.status),
     *        request,
     *        exception)
     *    .jsonError(s"Your account could not be authenticated. Reason: ${exception.resultCode}")
     *  }
     * }
     *
     * @param request the [[com.twitter.finagle.http.Request]] that triggered the failure
     * @param source  The named component responsible for causing this failure.
     * @param details Details about this exception suitable for stats. Each element will be converted into a string.
     *                Note: Each element must have a bounded set of values (e.g. You can stat the type of a tweet
     *                as "protected" or "unprotected", but you can't include the actual tweet id "696081566032723968").
     * @param message Details about this exception to be logged when this exception occurs. Typically logDetails
     *                contains the unbounded details of the exception that you are not able to stat such as an
     *                actual tweet ID (see above)
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
      failureClassifier(
        classifier = true,
        request,
        exception.source,
        exception.details,
        exception.message
      )
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
     * @param classifier if the failure should be "classified", e.g. logged and stat'd accordingly
     * @param request    the [[com.twitter.finagle.http.Request]] that triggered the failure
     * @param source     The named component responsible for causing this failure.
     * @param details    Details about this exception suitable for stats. Each element will be converted into a string.
     *                   Note: Each element must have a bounded set of values (e.g. You can stat the type of a tweet
     *                   as "protected" or "unprotected", but you can't include the actual tweet id "696081566032723968").
     * @param message    Details about this exception to be logged when this exception occurs. Typically logDetails
     *                   contains the unbounded details of the exception that you are not able to stat such as an
     *                   actual tweet ID (see above).
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
        serviceFailureScoped
          .scope(source)
          .counter(detailStrings: _*)
          .incr() // service/failure/AuthService/3040/Bad_signature
        RouteInfo(request) match {
          case Some(info) =>
            // route/hello/POST/failure/AuthService/3040/Bad_signature
            statsReceiver
              .scope("route", info.sanitizedPath, request.method.toString(), "failure")
              .scope(source)
              .counter(detailStrings: _*)
              .incr()
          case _ =>
          // No stored RouteInfo. Note: the com.twitter.finatra.http.exceptions.ExceptionManager
          // will always stat failure details for a request.
        }
      }

      this
    }

    /* Public Conversions */

    def toFuture: Future[Response] = Future.value(response)

    def toException: HttpResponseException = new HttpResponseException(response)

    def toFutureException[T]: Future[T] = Future.exception(toException)

    /* Private */

    private def getContentType = {
      response.headerMap.get(Fields.ContentType)
    }

    private def hasExtension(requestPath: String) = {
      fileResolver.getFileExtension(requestPath).nonEmpty
    }

    private def exists(path: String) = {
      val fileWithSlash = if (path.startsWith("/")) path else "/" + path
      fileResolver.exists(fileWithSlash)
    }

    private def body(request: Option[Request], any: Any): EnrichedResponse = {
      any match {
        case null => nothing
        case buf: Buf => body(buf)
        case bytes: Array[Byte] => body(bytes)
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
