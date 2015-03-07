package com.twitter.finatra.response

import com.google.common.net.{HttpHeaders, MediaType}
import com.twitter.finagle.http.{Cookie => FinagleCookie, Response, Status}
import com.twitter.finatra.internal.marshalling.MessageBodyManager
import com.twitter.finatra.internal.marshalling.mustache.MustacheService
import com.twitter.finatra.exceptions.HttpResponseException
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.routing.FileResolver
import com.twitter.util.{Future, Memoize}
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import javax.inject.Inject
import org.apache.commons.io.FilenameUtils._
import org.apache.commons.io.IOUtils
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.{Cookie => NettyCookie, DefaultCookie, HttpResponseStatus}
import scala.runtime.BoxedUnit

//TODO: Generate more response permutations
class ResponseBuilder @Inject()(
  objectMapper: FinatraObjectMapper,
  fileResolver: FileResolver,
  messageBodyManager: MessageBodyManager,
  mustacheService: MustacheService) {

  /* Status Codes */

  def status(statusCode: Int): EnrichedResponse = status(HttpResponseStatus.valueOf(statusCode))

  def status(httpResponseStatus: HttpResponseStatus): EnrichedResponse = EnrichedResponse(httpResponseStatus)

  def ok: EnrichedResponse = EnrichedResponse(Status.Ok)

  def ok(body: Any): EnrichedResponse = EnrichedResponse(Status.Ok).body(body)

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

  def badRequest = EnrichedResponse(Status.BadRequest)

  def badRequest(body: Any) = EnrichedResponse(Status.BadRequest).body(body)

  def conflict(body: Any) = EnrichedResponse(Status.Conflict).body(body)

  def unauthorized = EnrichedResponse(Status.Unauthorized)

  def unauthorized(body: Any) = EnrichedResponse(Status.Unauthorized).body(body)

  def forbidden = EnrichedResponse(Status.Forbidden)

  def forbidden(body: Any) = EnrichedResponse(Status.Forbidden).body(body)

  def notFound = EnrichedResponse(Status.NotFound)

  def notFound(body: String) = EnrichedResponse(Status.NotFound).plain(body)

  def notFound(body: Any) = EnrichedResponse(Status.NotFound).body(body)

  def gone = EnrichedResponse(Status.Gone)

  def gone(body: Any) = EnrichedResponse(Status.Gone).body(body)

  def internalServerError = EnrichedResponse(Status.InternalServerError)

  def internalServerError(body: Any) = EnrichedResponse(Status.InternalServerError).body(body)

  def notImplemented = EnrichedResponse(Status.NotImplemented)

  def serviceUnavailable = EnrichedResponse(Status.ServiceUnavailable)

  def clientClosed = EnrichedResponse(new HttpResponseStatus(499, "Client Closed Request"))

  /* Wrapper around Finagle Response which exposes a builder like API */
  case class EnrichedResponse(
    override val status: HttpResponseStatus)
    extends SimpleResponse(status) {

    /* Public */

    def cookie(k: String, v: String): EnrichedResponse = {
      cookie(new FinagleCookie(new DefaultCookie(k, v)))
      this
    }

    def cookie(c: FinagleCookie): EnrichedResponse = {
      httpResponse.addCookie(c)
      this
    }

    def cookie(c: NettyCookie): EnrichedResponse = {
      httpResponse.addCookie(new FinagleCookie(c))
      this
    }

    def json(obj: Any) = {
      contentTypeJson()

      obj match {
        case bytes: Array[Byte] => body(bytes)
        case str: String => body(str)
        case _ =>
          httpResponse.withOutputStream { os =>
            objectMapper.writeValue(obj, os)
          }
      }
      this
    }

    def body(any: Any): EnrichedResponse = {
      any match {
        case null => nothing
        case bytes: Array[Byte] => body(bytes)
        case cbos: ChannelBuffer => body(cbos)
        case "" => nothing
        case Unit => nothing
        case _: BoxedUnit => nothing
        case opt if opt == None => nothing
        case str: String => body(str)
        case _file: File => file(_file)
        case _ =>
          val writer = messageBodyManager.writerOrDefault(any)
          val writerResponse = writer.write(any)
          body(writerResponse.body)
          contentType(writerResponse.contentType)
          headers(writerResponse.headers)
      }
      this
    }

    def file(file: File): EnrichedResponse = {
      body(
        new BufferedInputStream(
          new FileInputStream(file)))

      contentType(
        fileResolver.getContentType(file.getName))
    }

    def body(b: Array[Byte]): EnrichedResponse = {
      httpResponse.setContent(wrappedBuffer(b))
      this
    }

    def body(bodyStr: String): EnrichedResponse = {
      if (bodyStr == "") {
        nothing
      }
      else {
        httpResponse.setContentString(bodyStr)
        this
      }
    }

    def body(inputStream: InputStream): EnrichedResponse = {
      body(
        ChannelBuffers.wrappedBuffer(
          IOUtils.toByteArray(
            inputStream)))

      this
    }

    def body(channelBuffer: ChannelBuffer): EnrichedResponse = {
      httpResponse.setContent(channelBuffer)
      this
    }

    def contentTypeJson() = {
      contentType("application/json")
      this
    }

    def nothing = {
      httpResponse.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8)
      this
    }

    def plain(any: Any): EnrichedResponse = {
      httpResponse.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8)
      body(any)
    }

    def html(body: String) = {
      httpResponse.headers().set("Content-Type", "text/html")
      httpResponse.setContentString(body)
      this
    }

    def html(obj: Any) = {
      httpResponse.headers().set("Content-Type", "text/html")
      body(obj)
      this
    }

    def location(uri: Any): EnrichedResponse = {
      location(uri.toString)
    }

    def location(uri: String): EnrichedResponse = {
      httpResponse.headers().set("Location", uri)
      this
    }

    def header(k: String, v: Any) = {
      httpResponse.headers().set(k, v)
      this
    }

    def header(k: String, v: MediaType) = {
      httpResponse.headers().set(k, v)
      this
    }

    def headers(map: Map[String, String]) = {
      for ((k, v) <- map) {
        httpResponse.headers().set(k, v)
      }
      this
    }

    def headers(entries: (String, Any)*) = {
      for ((k, v) <- entries) {
        httpResponse.headers().set(k, v)
      }
      this
    }

    def contentType(mimeType: String) = {
      httpResponse.headers().set(CONTENT_TYPE, mimeType + ";charset=utf-8")
      this
    }

    def contentType(mimeType: MediaType) = {
      httpResponse.headers().set(
        CONTENT_TYPE,
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
      html(mustacheService.createChannelBuffer(template, obj))
    }

    def toFuture: Future[Response] = Future.value(this)

    def toException: HttpResponseException = new HttpResponseException(this)

    def toFutureException[T]: Future[T] = Future.exception(toException)

    /* Private */

    private def isFile(requestPath: String) = {
      getExtension(requestPath).nonEmpty
    }

    //optimized: MediaType.toString is a hotspot when profiling
    private val mediaToString = Memoize { mediaType: MediaType =>
      mediaType.toString
    }
  }

}
