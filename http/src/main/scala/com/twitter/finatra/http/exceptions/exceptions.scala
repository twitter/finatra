package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.Status

/* HTTP Exceptions */
// TODO: Redesign to avoid boilerplate below (@see ResponseBuilder) */

/**
 * HttpException which will be rendered as an HTTP response.
 */
object HttpException {
  def plainText(status: Status, body: String): HttpException = {
    new HttpException(status, MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(status: Status, errors: String*): HttpException = {
    new HttpException(status, MediaType.JsonUtf8, errors)
  }
}

class HttpException(
  val statusCode: Status,
  val mediaType: String,
  val errors: Seq[String] = Seq(),
  val headers: Seq[(String, String)] = Seq()
) extends Exception {

  override def getMessage: String = {
    "HttpException(" + statusCode + ":" + mediaType + ") with errors: " + errors.mkString(",") + {
      if (headers.isEmpty) ""
      else ", with headers: " + headers.mkString(",")
    }
  }

  /* Generated Equals/Hashcode */

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

/* Specific Status Exceptions */

object NotFoundException {
  def plainText(body: String): NotFoundException = {
    new NotFoundException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*):NotFoundException = {
    new NotFoundException(MediaType.JsonUtf8, errors)
  }
}

case class NotFoundException(override val mediaType: String, override val errors: Seq[String])
    extends HttpException(Status.NotFound, mediaType, errors) {

  def this(error: String) = {
    this(MediaType.JsonUtf8, Seq(error))
  }
}

object ConflictException {
  def plainText(body: String): ConflictException = {
    new ConflictException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): ConflictException = {
    new ConflictException(MediaType.JsonUtf8, errors)
  }
}

case class ConflictException(override val mediaType: String, override val errors: Seq[String])
    extends HttpException(Status.Conflict, mediaType, errors) {

  def this(error: String) = {
    this(MediaType.JsonUtf8, Seq(error))
  }
}

object InternalServerErrorException {
  def plainText(body: String): InternalServerErrorException = {
    new InternalServerErrorException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): InternalServerErrorException = {
    new InternalServerErrorException(MediaType.JsonUtf8, errors)
  }
}

case class InternalServerErrorException(
  override val mediaType: String,
  override val errors: Seq[String]
) extends HttpException(Status.InternalServerError, mediaType, errors)

object ServiceUnavailableException {
  def plainText(body: String): ServiceUnavailableException = {
    new ServiceUnavailableException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): ServiceUnavailableException = {
    new ServiceUnavailableException(MediaType.JsonUtf8, errors)
  }
}

case class ServiceUnavailableException(
  override val mediaType: String,
  override val errors: Seq[String]
) extends HttpException(Status.ServiceUnavailable, mediaType, errors)

object BadRequestException {
  def plainText(body: String): BadRequestException = {
    new BadRequestException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): BadRequestException = {
    new BadRequestException(MediaType.JsonUtf8, errors)
  }
}

case class BadRequestException(override val mediaType: String, override val errors: Seq[String])
    extends HttpException(Status.BadRequest, mediaType, errors) {

  def this(error: String) = {
    this(MediaType.JsonUtf8, Seq(error))
  }
}

object ForbiddenException {
  def plainText(body: String): ForbiddenException = {
    new ForbiddenException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): ForbiddenException = {
    new ForbiddenException(MediaType.JsonUtf8, errors)
  }
}

case class ForbiddenException(override val mediaType: String, override val errors: Seq[String])
    extends HttpException(Status.Forbidden, mediaType, errors)

object NotAcceptableException {
  def plainText(body: String): NotAcceptableException = {
    new NotAcceptableException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): NotAcceptableException = {
    new NotAcceptableException(MediaType.JsonUtf8, errors)
  }
}

case class NotAcceptableException(
  override val mediaType: String,
  override val errors: Seq[String]
) extends HttpException(Status.NotAcceptable, mediaType, errors)

object MethodNotAllowedException {
  def plainText(body: String): MethodNotAllowedException = {
    new MethodNotAllowedException(MediaType.PlainTextUtf8, Seq(body))
  }

  def apply(errors: String*): MethodNotAllowedException = {
    new MethodNotAllowedException(MediaType.JsonUtf8, errors)
  }
}

case class MethodNotAllowedException(
  override val mediaType: String,
  override val errors: Seq[String]
) extends HttpException(Status.MethodNotAllowed, mediaType, errors) {
  def this(error: String) = {
    this(MediaType.JsonUtf8, Seq(error))
  }
}
