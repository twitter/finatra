package com.twitter.finatra.http.filters

import com.twitter.conversions.time._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.util.{ScheduledThreadPoolTimer, Future}
import java.util.{Locale, TimeZone}
import javax.inject.Singleton
import org.apache.commons.lang.time.FastDateFormat

/**
 * HttpResponseFilter does the following:
 * - sets the 'Server' and 'Date' response headers
 * - turns a 'partial' Location header into a full URL
 */
@Singleton
class HttpResponseFilter[R <: Request] extends SimpleFilter[R, Response] {

  // optimized
  private val dateFormat = FastDateFormat.getInstance(
    HttpHeaders.RFC7231DateFormat,
    TimeZone.getTimeZone("GMT"),
    Locale.ENGLISH)
  @volatile private var currentDateValue: String = getCurrentDateValue()
  new ScheduledThreadPoolTimer(
    poolSize = 1,
    name = "HttpDateUpdater",
    makeDaemons = true)
    .schedule(1.second) {
      currentDateValue = getCurrentDateValue()
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
   * @see Date: <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
   * @see Server: <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
   * @see Content-Type: <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
   * @param response - the response on which to set the header values.
   */
  private def setResponseHeaders(response: Response) = {
    response.headerMap.add(HttpHeaders.Server, "Finatra")
    response.headerMap.add(HttpHeaders.Date, currentDateValue)
    if (response.contentType.isEmpty && response.length != 0) {
      // see: https://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.2.1
      response.headerMap.add(HttpHeaders.ContentType, "application/octet-stream")
    }
  }

  private def getCurrentDateValue(): String = {
    dateFormat.format(System.currentTimeMillis())
  }

  private def updateLocationHeader(request: R, response: Response) = {
    for (existingLocation <- response.location) {
      if (!existingLocation.startsWith("http") && !existingLocation.startsWith("/")) {
        response.headerMap.set(
          HttpHeaders.Location,
          RequestUtils.pathUrl(request) + existingLocation)
      }
    }
  }
}
