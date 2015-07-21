package com.twitter.finatra.http.request

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.finatra.http.fileupload.MultipartItem
import com.twitter.finatra.http.internal.marshalling.FinatraFileUpload
import org.jboss.netty.handler.codec.http.HttpHeaders

object RequestUtils {

  /** Fully qualified requested URL with ending slash and no query params (suitable for location header creation) */
  def pathUrl(request: Request): String = {
    val scheme = HttpHeaders.getHeader(request.httpMessage, "x-forwarded-proto", "http")
    val hostHeader = request.host getOrElse (throw new BadRequestException("Host header not set"))
    val pathWithTrailingSlash = if (request.path.endsWith("/")) request.path else request.path + "/"
    scheme + "://" + hostHeader + pathWithTrailingSlash
  }

  /** Multipart parsed params */
  def multiParams(request: Request): Map[String, MultipartItem] = {
    new FinatraFileUpload().parseMultipartItems(request)
  }
}
