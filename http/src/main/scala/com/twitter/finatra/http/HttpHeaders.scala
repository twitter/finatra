package com.twitter.finatra.http

import com.twitter.finagle.http.Response
import java.util.Locale
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}


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

  val GMT = DateTimeZone.forID("GMT")

  /**
   * RFC 7231 Date Format
   * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.1.1">RFC 7231 Section 7.1.1.1</a>
   */
  val RFC7231DateFormat = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"

  private val DateFormat = DateTimeFormat
    .forPattern(RFC7231DateFormat)
    .withZone(GMT)
    .withLocale(Locale.US)

  /**
   * Set the given date under the given header name after formatting it as a string
   * using the pattern {@code RFC7231DateFormat}.
   * @param response - the Response on which to set the formatted Date header
   * @param header - the name of the header to set
   * @param date - the value to format and set as the header value
   */
  def setDate(response: Response, header: String, date: DateTime) {
    set(response, header, DateFormat.print(date))
  }

  /**
   * Set the given single header value for the given header name.
   * @param response - the Response on which to set the formatted Date header
   * @param header - the name of the header to set
   * @param value - the value to set for the given header
   */
  def set(response: Response, header: String, value: String): Unit = {
    response.headerMap.set(header, value)
  }
}
