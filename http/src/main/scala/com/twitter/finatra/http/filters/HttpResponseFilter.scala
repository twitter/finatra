package com.twitter.finatra.http.filters

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.util.Future
import javax.inject.Singleton

/**
 * HttpResponseFilter does the following:
 * - setting the HTTP protocol version
 * - turning a 'partial' Location header into a full URL
 */
@Singleton
class HttpResponseFilter[R <: Request] extends SimpleFilter[R, Response] {

  private val locationStatusCodes = Set(
    Created, Accepted, //2XX
    MultipleChoices, MovedPermanently, Found, SeeOther, NotModified, UseProxy, TemporaryRedirect) //3XX

  /* Public */

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    for (response <- service(request)) yield {
      response.setProtocolVersion(request.version)
      updateLocationHeader(request, response)
      response
    }
  }

  /* Private */

  private def updateLocationHeader(request: R, response: Response) {
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
