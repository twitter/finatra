package com.twitter.finatra.httpclientv2

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.inject.conversions.future._
import com.twitter.inject.utils.RetryUtils
import com.twitter.util.{Future, Try}

/**
 * A simple HTTP client.
 *
 * @note Some servers won't handle requests properly if the Host header is not set
 * @param hostname the hostname that will be used for the Host header. Leave as default or set as "" to not set a Host header
 * @param httpService underlying `com.twitter.finagle.Service`
 * @param retryPolicy optional retry policy if the service fails to get a successful response
 * @param defaultHeaders headers to add to every request
 */
class HttpClient(
  hostname: String = "",
  httpService: Service[Request, Response],
  retryPolicy: Option[RetryPolicy[(Request, Try[Response])]] = None,
  defaultHeaders: Map[String, String] = Map(),
) extends Logging {

  /* Public */

  def execute(request: Request): Future[Response] = {
    debug(request + " with headers: " + request.headerMap.mkString(", "))
    setHeaders(request)
    setHostname(request)

    retryPolicy match {
      case Some(policy) => RetryUtils.retryFuture(policy)(httpService(request))
      case _ => httpService(request)
    }
  }

  /* Private */

  private def setHostname(request: Request) = {
    if (hostname.nonEmpty) {
      request.headerMap.set("Host", hostname)
    }
  }

  private def setHeaders(request: Request): Unit = {
    if (defaultHeaders.nonEmpty) {
      for ((key, value) <- defaultHeaders) {
        request.headerMap.set(key, value)
      }
    }
  }
}
