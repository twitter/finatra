package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.{MediaType, Status}

object HttpException {
  def plainText(status: Status, body: String): HttpException = {
    new HttpException(status, MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(status: Status, errors: String*): HttpException = {
    new HttpException(status, MediaType.JsonUtf8, errors)
  }
}

/**
 * An [[Exception]] which will be rendered as an HTTP response.
 */
class HttpException(
  val statusCode: Status,
  val mediaType: String,
  val errors: Seq[String] = Seq(),
  val headers: Seq[(String, String)] = Seq())
    extends Exception {

  override def getMessage: String = {
    "HttpException(" + statusCode + ":" + mediaType + ") with errors: " + errors.mkString(",") + {
      if (headers.isEmpty) ""
      else ", with headers: " + headers.mkString(",")
    }
  }

  override def equals(other: Any): Boolean = other match {
    case that: HttpException =>
      (that canEqual this) &&
        statusCode == that.statusCode &&
        mediaType == that.mediaType &&
        errors == that.errors &&
        headers == that.headers
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(statusCode, mediaType, errors, headers)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[HttpException]
}
