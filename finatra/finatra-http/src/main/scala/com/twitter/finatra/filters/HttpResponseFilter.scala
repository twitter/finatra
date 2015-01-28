package com.twitter.finatra.filters

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.request.RequestUtils
import com.twitter.util.Future

/**
 * HttpResponseFilter does the following:
 * - setting the HTTP protocol version
 * - turning a 'partial' Location header into a full URL
 */
class HttpResponseFilter extends SimpleFilter[Request, Response] {

  private val locationStatusCodes = Set(
    Created, Accepted, //2XX
    MultipleChoices, MovedPermanently, Found, SeeOther, NotModified, UseProxy, TemporaryRedirect) //3XX

  /* Public */

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    for (response <- service(request)) yield {
      response.setProtocolVersion(request.version)
      updateLocationHeader(request, response)
      response
    }
  }

  /* Private */

  private def updateLocationHeader(request: Request, response: Response) {
    if (locationStatusCodes.contains(response.status)) {
      for (existingLocation <- response.location) {
        if (!existingLocation.startsWith("http") && !existingLocation.startsWith("/")) {
          response.headers.set(
            "Location",
            RequestUtils.pathUrl(request) + existingLocation)
        }
      }
    }
  }
}
