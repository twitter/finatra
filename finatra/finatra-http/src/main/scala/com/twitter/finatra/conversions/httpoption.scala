package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.options.RichOption
import com.twitter.finatra.exceptions._

object httpoption {

  class HttpRichOption[A](wrapped: Option[A]) {
    def valueOrNotFound(msg: String = ""): A = {
      wrapped.getOrElse(throw new NotFoundException(msg))
    }

    def toFutureOrNotFound(msg: String = "") = {
      RichOption.toFutureOrFail(wrapped, NotFoundException(msg))
    }

    def toFutureOrBadRequest(msg: String = "") = {
      RichOption.toFutureOrFail(wrapped, BadRequestException(msg))
    }

    def toFutureOrServerError(msg: String = "") = {
      RichOption.toFutureOrFail(wrapped, InternalServerErrorException(msg))
    }

    def toFutureOrForbidden(msg: String = "") = {
      RichOption.toFutureOrFail(wrapped, ForbiddenException(msg))
    }

    def toTryOrServerError(msg: String = "") = {
      RichOption.toTryOrFail(wrapped, InternalServerErrorException(msg))
    }
  }

  implicit def richOption[A](wrapped: Option[A]): HttpRichOption[A] = new HttpRichOption[A](wrapped)
}
