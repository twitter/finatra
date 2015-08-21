package com.twitter.finatra.http.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finagle.httpx.Status._
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.util.Future
import javax.inject.Singleton
import org.joda.time.DateTime

/**
 * HttpResponseFilter does the following:
 * - sets the HTTP protocol version
 * - sets the 'Server' and 'Date' response headers
 * - turns a 'partial' Location header into a full URL
 */
@Singleton
class HttpResponseFilter[R <: Request] extends SimpleFilter[R, Response] {

  private val locationStatusCodes = Set(
    Created, Accepted, //2XX
    MultipleChoices, MovedPermanently, Found, SeeOther, NotModified, UseProxy, TemporaryRedirect) //3XX

  /* Public */

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    for (response <- service(request)) yield {
      response.version = request.version
      setResponseHeaders(response)
      updateLocationHeader(request, response)
      response
    }
  }

  /* Private */

  private def updateLocationHeader(request: R, response: Response) {
    if (locationStatusCodes.contains(response.status)) {
      for (existingLocation <- response.location) {
        if (!existingLocation.startsWith("http") && !existingLocation.startsWith("/")) {
          response.headerMap.set(
            "Location",
            RequestUtils.pathUrl(request) + existingLocation)
        }
      }
    }
  }

  /**
   * Sets the HTTP Date and Server header values.
   * @see Date: <a href="http://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
   * @see Server <a href="http://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
   * @param response- the response on which to set the header values.
   */
  private def setResponseHeaders(response: Response) {
    HttpHeaders.set(response, HttpHeaders.Server, "Finatra")
    HttpHeaders.setDate(response, HttpHeaders.Date, DateTime.now)
  }
}
