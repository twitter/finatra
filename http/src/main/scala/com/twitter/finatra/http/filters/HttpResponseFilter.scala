package com.twitter.finatra.http.filters

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{MediaType, Message, Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, ScheduledThreadPoolTimer, Throw, Try}
import java.net.URI
import javax.inject.Singleton

/**
 * HttpResponseFilter does the following:
 *  - sets the 'Server' and 'Date' response headers
 *  - turns a 'partial' Location header into a full URL
 */
@Singleton
class HttpResponseFilter[R <: Request] extends SimpleFilter[R, Response] with Logging {

  // optimized
  @volatile private var currentDateValue: String = getCurrentDateValue
  new ScheduledThreadPoolTimer(poolSize = 1, name = "HttpDateUpdater", makeDaemons = true)
    .schedule(1.second) {
      currentDateValue = getCurrentDateValue
    }

  /* Public */

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    for (response <- service(request)) yield {
      setResponseHeaders(response)
      updateLocationHeader(request, response)
      response
    }
  }

  /* Private */

  /**
   * Sets the HTTP Date and Server header values. If there is no Content-type header in the response, but a non-zero
   * content length, we also set to the generic: application/octet-stream content type on the response.
   *
   * @param response - the response on which to set the header values.
   *
   * @see Date: [[https://tools.ietf.org/html/rfc7231#section-7.1.1.2 Section 7.1.1.2 of RFC 7231]]
   * @see Server: [[https://tools.ietf.org/html/rfc7231#section-7.4.2 Section 7.4.2 of RFC 7231]]
   * @see Content-Type: [[https://tools.ietf.org/html/rfc7231#section-3.1.1.5 Section 3.1.1.5 of RFC 7231]]
   */
  private def setResponseHeaders(response: Response): Unit = {
    response.headerMap.setUnsafe(HttpHeaders.Server, "Finatra")
    response.headerMap.setUnsafe(HttpHeaders.Date, currentDateValue)
    if (response.contentType.isEmpty && response.length != 0) {
      // see: https://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.2.1
      response.headerMap.setUnsafe(HttpHeaders.ContentType, MediaType.OctetStream)
    }
  }

  private def getCurrentDateValue: String = Message.httpDateFormat(System.currentTimeMillis())

  private def updateLocationHeader(request: R, response: Response): Unit = {
    for (existingLocation <- response.location) {
      Try(new URI(existingLocation)) match {
        case Throw(e) =>
          warn(
            s"Response location header value $existingLocation is not a valid URI. ${e.getMessage}"
          )
        case Return(uri) if uri.getScheme == null =>
          response.headerMap.set(
            HttpHeaders.Location,
            RequestUtils.normalizedURIWithoutScheme(uri, request)
          )
        case _ =>
        // don't modify
      }
    }
  }
}
