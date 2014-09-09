package com.twitter.finatra.filters.general

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.request.RequestUtils
import com.twitter.util.Future

/**
 * HttpFilter which does a few of 'HTTP level' things
 * - setting the HTTP protocol version
 * - adding a special TFE header for preserving response bodies on 4xx/5xx responses
 * - turning a 'partial' Location header into a full URL
 */
//TODO: DATAAPI-792 Remove "twitter internal" headers
class HttpResponseFilter extends SimpleFilter[Request, Response] {

  /* Public */

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    for (response <- service(request)) yield {
      response.setProtocolVersion(request.version)
***REMOVED***
      updateLocationHeader(request, response)
      response
    }
  }

  /* Private */

  private def updateLocationHeader(request: Request, response: Response) {
    for (existingLocation <- response.location) {
      if (!existingLocation.startsWith("http") && !existingLocation.startsWith("/")) {
        response.headers.set("Location", RequestUtils.pathUrl(request) + existingLocation)
      }
    }
  }
}

