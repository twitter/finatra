package com.twitter.finatra.http

@deprecated("Use com.twitter.finagle.http.Fields", "2019-10-19")
object HttpHeaders {
  /**
   * HTTP `'''Content-Type'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-3.1.1.5 RFC 7231 Section 3.1.1.5]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.ContentType", "2019-10-19")
  val ContentType = "Content-Type"

  /**
   * HTTP `'''Content-Length'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7230#section-3.3.2 RFC 7230 Section 3.3.2]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.ContentLength", "2019-10-19")
  val ContentLength = "Content-Length"

  /**
   * HTTP `'''Date'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.1.1.2 RFC 7231 Section 7.1.1.2]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.Date", "2019-10-19")
  val Date = "Date"

  /**
   * HTTP `'''Server'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.4.2 RFC 7231 Section 7.4.2]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.Server", "2019-10-19")
  val Server = "Server"

  /**
   * HTTP `'''Accept'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-5.3.2 RFC 7231 Section 5.3.2]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.Accept", "2019-10-19")
  val Accept = "Accept"

  /**
   * HTTP `'''Retry-After'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.1.3 RFC 7231 Section 7.1.3]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.RetryAfter", "2019-10-19")
  val RetryAfter = "Retry-After"

  /**
   * HTTP `'''Location'''` header field name.
   * @see [[https://tools.ietf.org/html/rfc7231#section-7.1.2 RFC 7231 Section 7.1.2]]
   */
  @deprecated("Use com.twitter.finagle.http.Fields.Location", "2019-10-19")
  val Location = "Location"
}
