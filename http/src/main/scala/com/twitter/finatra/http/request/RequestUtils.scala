package com.twitter.finatra.http.request

import com.google.common.net.{MediaType => CommonMediaTypes}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.exceptions.{BadRequestException, NotAcceptableException}
import com.twitter.finatra.http.fileupload.{FinagleRequestFileUpload, MultipartItem}
import com.twitter.finatra.request.ContentType

object RequestUtils {

  /** Fully qualified requested URL with ending slash and no query params (suitable for location header creation) */
  def pathUrl(request: Request): String = {
    val scheme = request.headerMap.get("x-forwarded-proto") match {
      case Some(protocol) => protocol
      case _              => "http"
    }

    val hostHeader = request.host match {
      case Some(host) => host
      case _          => throw new BadRequestException("Host header not set")
    }

    val pathWithTrailingSlash = if (request.path.endsWith("/")) request.path else request.path + "/"
    scheme + "://" + hostHeader + pathWithTrailingSlash
  }

  /** Multipart parsed params */
  def multiParams(request: Request): Map[String, MultipartItem] = {
    new FinagleRequestFileUpload().parseMultipartItems(request)
  }

  /**
   * Content Negotiation
   * Example Accept Header format :
   * Accept: text/plain; q=0.5, text/html,
   * text/html; q=0.8, application/json
   */
  def respondTo[T](request: Request)(callback: PartialFunction[ContentType, T]): T = {
    val acceptHeader = request.headerMap.getOrElse(HttpHeaders.Accept, "*/*")
    val mediaRanges = MediaRange.parseAndSort(acceptHeader)

    val contentTypes = mediaRanges map { mediaRange =>
      ContentType.fromString(mediaRange.contentType)
    }
    contentTypes.collectFirst(callback).getOrElse(
      throw new NotAcceptableException(
        CommonMediaTypes.PLAIN_TEXT_UTF_8,
        Seq("Not Acceptable Media Type")))
  }
}
