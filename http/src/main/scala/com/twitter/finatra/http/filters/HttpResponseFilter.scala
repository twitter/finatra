package com.twitter.finatra.http.filters

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Fields, MediaType, Message, Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, ScheduledThreadPoolTimer, Throw, Try}
import java.net.URI
import javax.inject.Singleton

/**
 * HttpResponseFilter does the following:
 *  - Sets the 'Server' and 'Date' response header values.
 *  - Optionally turns a relative 'Location' header value into a full URL. See [[fullyQualifyLocationHeader]].
 *
 * By default this filter allows for returning relative references as 'Location' header values. In
 * order to always attempt to fully specify a relative reference, this class should be instantiated
 * with the constructor arg [[fullyQualifyLocationHeader]] set to 'true'.
 *
 *  @param fullyQualifyLocationHeader the Filter should attempt to always fully qualify
 *                                    the value of the response 'Location' header. Default: false
 *
 * @see [[https://en.wikipedia.org/wiki/HTTP_location HTTP location]]
 * @see [[https://tools.ietf.org/html/rfc7231 Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content]]
 * @see [[https://tools.ietf.org/html/rfc3986#section-4.2 Relative Reference]]
 */
@Singleton
class HttpResponseFilter[R <: Request](fullyQualifyLocationHeader: Boolean)
    extends SimpleFilter[R, Response]
    with Logging {

  def this() = {
    // https://tools.ietf.org/html/rfc7231#section-7.1.2 relaxes the requirement for complete
    // absolute URI values and allows for relative URLs in 'Location' headers. However, some servers
    // may want to always attempt to fully qualify any returned relative URL.
    this(fullyQualifyLocationHeader = false)
  }

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
    response.headerMap.setUnsafe(Fields.Server, "Finatra")
    response.headerMap.setUnsafe(Fields.Date, currentDateValue)
    if (response.contentType.isEmpty && response.length != 0) {
      // see: https://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.2.1
      response.headerMap.setUnsafe(Fields.ContentType, MediaType.OctetStream)
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
        case Return(uri) if isRelativeLocation(uri) =>
          response.headerMap.set(
            Fields.Location,
            getLocationHeaderValue(fullyQualify = this.fullyQualifyLocationHeader, uri, request)
          )
        case Return(uri) =>
          // valid URI with a non-null scheme, check scheme against "x-forwarded-proto" from request
          request.headerMap.get("x-forwarded-proto") match {
            case Some(protocol) if protocol != uri.getScheme =>
              response.headerMap.set(
                Fields.Location,
                getLocationHeaderValue(fullyQualify = true, uri, request)
              )
            case _ =>
            // valid URI with a non-null scheme, no "x-forwarded-proto" from request = do nothing
          }
      }
    }
  }

  // uri.getScheme == null is a relative reference
  private[this] def isRelativeLocation(uri: URI): Boolean = uri.getScheme == null

  private def getLocationHeaderValue(fullyQualify: Boolean, uri: URI, request: Request): String = {
    val normalizedURI = uri.normalize()

    val scheme: String =
      if (fullyQualify) s"${RequestUtils.getScheme(request)}:"
      else if (normalizedURI.getScheme == null || normalizedURI.getScheme.isEmpty) ""
      else
        s"${normalizedURI.getScheme}:" // we don't expect to get to this case but it is here for completeness
    val authority: String =
      if (fullyQualify) s"//${RequestUtils.getAuthority(request, normalizedURI)}"
      else getNormalizedURIAuthority(normalizedURI)
    val path: String = RequestUtils.getPath(request, normalizedURI.getPath)
    val query: String = RequestUtils.getQuery(normalizedURI)
    val fragment: String = RequestUtils.getFragment(normalizedURI)

    s"$scheme$authority$path$query$fragment"
  }

  private[this] def getNormalizedURIAuthority(normalizedURI: URI): String =
    Option(normalizedURI.getAuthority) match {
      case Some(authority) => "//" + authority
      case _ => ""
    }
}
