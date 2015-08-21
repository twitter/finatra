package com.twitter.finatra.http.request

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.finatra.http.fileupload.MultipartItem
import com.twitter.finatra.http.internal.marshalling.FinatraFileUpload

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
    new FinatraFileUpload().parseMultipartItems(request)
  }
}
