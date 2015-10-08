package com.twitter.finatra.http.exceptions

import com.google.common.net.MediaType
import com.twitter.finagle.http.Status

/* HTTP Exceptions */
// TODO: Redesign to avoid boilerplate below (@see ResponseBuilder) */

/**
 * HttpException which will be rendered as an HTTP response.
 */
object HttpException {
  def plainText(status: Status, body: String) = {
    new HttpException(status, MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(status: Status, errors: String*) = {
    new HttpException(status, MediaType.JSON_UTF_8, errors)
  }
}

class HttpException(
  val statusCode: Status,
  val mediaType: MediaType,
  val errors: Seq[String] = Seq())
  extends Exception {

  /* Public */

  override def getMessage: String = {
    "HttpException(" + statusCode + ":" + mediaType + ") with errors: " + errors.mkString(",")
  }

  /* Generated Equals/Hashcode */

  override def equals(other: Any): Boolean = other match {
    case that: HttpException =>
      (that canEqual this) &&
        statusCode == that.statusCode &&
        mediaType == that.mediaType &&
        errors == that.errors
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(statusCode, mediaType, errors)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[HttpException]
}

/* Specific Status Exceptions */

object NotFoundException {
  def plainText(body: String) = {
    new NotFoundException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new NotFoundException(MediaType.JSON_UTF_8, errors)
  }
}

case class NotFoundException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.NotFound, mediaType, errors) {

  def this(error: String) = {
    this(MediaType.JSON_UTF_8, Seq(error))
  }
}

object ConflictException {
  def plainText(body: String) = {
    new ConflictException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new ConflictException(MediaType.JSON_UTF_8, errors)
  }
}

case class ConflictException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.Conflict, mediaType, errors) {

  def this(error: String) = {
    this(MediaType.JSON_UTF_8, Seq(error))
  }
}


object InternalServerErrorException {
  def plainText(body: String) = {
    new InternalServerErrorException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new InternalServerErrorException(MediaType.JSON_UTF_8, errors)
  }
}

case class InternalServerErrorException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.InternalServerError, mediaType, errors)

object ServiceUnavailableException {
  def plainText(body: String) = {
    new ServiceUnavailableException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new ServiceUnavailableException(MediaType.JSON_UTF_8, errors)
  }
}

case class ServiceUnavailableException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.ServiceUnavailable, mediaType, errors)


object BadRequestException {
  def plainText(body: String) = {
    new BadRequestException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new BadRequestException(MediaType.JSON_UTF_8, errors)
  }
}

case class BadRequestException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.BadRequest, mediaType, errors) {

  def this(error: String) = {
    this(MediaType.JSON_UTF_8, Seq(error))
  }
}


object ForbiddenException {
  def plainText(body: String) = {
    new ForbiddenException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new ForbiddenException(MediaType.JSON_UTF_8, errors)
  }
}

case class ForbiddenException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.Forbidden, mediaType, errors)


object NotAcceptableException {
  def plainText(body: String) = {
    new NotAcceptableException(MediaType.PLAIN_TEXT_UTF_8, Seq(body))
  }

  def apply(errors: String*) = {
    new NotAcceptableException(MediaType.JSON_UTF_8, errors)
  }
}

case class NotAcceptableException(
  override val mediaType: MediaType,
  override val errors: Seq[String])
  extends HttpException(Status.NotAcceptable, mediaType, errors)
