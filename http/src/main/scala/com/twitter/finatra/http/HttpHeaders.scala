package com.twitter.finatra.http

object HttpHeaders {

  /**
   * HTTP `'''Content-Type'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-3.1.1.5 RFC 7231 Section 3.1.1.5]]
   */
  val ContentType = "Content-Type"

  /**
   * HTTP `'''Content-Length'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7230#section-3.3.2 RFC 7230 Section 3.3.2]]
   */
  val ContentLength = "Content-Length"

  /**
   * HTTP `'''Date'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.1.1.2 RFC 7231 Section 7.1.1.2]]
   */
  val Date = "Date"

  /**
   * HTTP `'''Server'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.4.2 RFC 7231 Section 7.4.2]]
   */
  val Server = "Server"

  /**
   * HTTP `'''Accept'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-5.3.2 RFC 7231 Section 5.3.2]]
   */
  val Accept = "Accept"

  /**
   * HTTP `'''Retry-After'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.1.3 RFC 7231 Section 7.1.3]]
   */
  val RetryAfter = "Retry-After"

  /**
   * HTTP `'''Location'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.1.2 RFC 7231 Section 7.1.2]]
   */
  val Location = "Location"

  /**
   * RFC 7231 Date Format
   * @see [[http://tools.ietf.org/html/rfc7231#section-7.1.1.1 RFC 7231 Section 7.1.1.1]]
   */
  val RFC7231DateFormat = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"

  /**
   * HTTP `'''Canonical-Resource'''` header field name, used in Diffy Proxy
   * @see [[https://github.com/twitter/diffy/tree/master/example Diffy Project]]
   */
  val CanonicalResource = "Canonical-Resource"
}
