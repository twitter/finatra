package com.twitter.finatra.http

object HttpHeaders {

  /**
   * HTTP {@code Content-Type} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC 7231 Section 3.1.1.5</a>
   */
  val ContentType = "Content-Type"

  /**
   * HTTP {@code Content-Length} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">RFC 7230 Section 3.3.2</a>
   */
  val ContentLength = "Content-Length"

  /**
   * HTTP {@code Date} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">RFC 7231 Section 7.1.1.2</a>
   */
  val Date = "Date"

  /**
   * HTTP {@code Server} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">RFC 7231 Section 7.4.2</a>
   */
  val Server = "Server"

  /**
   * HTTP {@code Accept} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
   */
  val Accept = "Accept"

  /**
   * HTTP {@code Retry-After} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231 Section 7.1.3</a>
   */
  val RetryAfter = "Retry-After"

  /**
   * HTTP {@codeLocation} header field name.
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">RFC 7231 Section 7.1.2</a>
   */
  val Location = "Location"

  /**
   * RFC 7231 Date Format
   * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.1.1">RFC 7231 Section 7.1.1.1</a>
   */
  val RFC7231DateFormat = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
}
