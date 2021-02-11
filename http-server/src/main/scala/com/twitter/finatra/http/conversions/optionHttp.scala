package com.twitter.finatra.http.conversions

import com.twitter.finatra.http.exceptions._
import com.twitter.inject.conversions.option.RichOption
import com.twitter.util.{Future, Try}

object optionHttp {

  /* -------------------------------------------------------- */
  implicit class HttpRichOption[A](val self: Option[A]) extends AnyVal {
    def valueOrNotFound(msg: String = ""): A = {
      self.getOrElse(throw new NotFoundException(msg))
    }

    def toFutureOrNotFound(msg: String = ""): Future[A] = {
      RichOption.toFutureOrFail(self, NotFoundException(msg))
    }

    def toFutureOrBadRequest(msg: String = ""): Future[A] = {
      RichOption.toFutureOrFail(self, BadRequestException(msg))
    }

    def toFutureOrServerError(msg: String = ""): Future[A] = {
      RichOption.toFutureOrFail(self, InternalServerErrorException(msg))
    }

    def toFutureOrForbidden(msg: String = ""): Future[A] = {
      RichOption.toFutureOrFail(self, ForbiddenException(msg))
    }

    def toTryOrServerError(msg: String = ""): Try[A] = {
      RichOption.toTryOrFail(self, InternalServerErrorException(msg))
    }
  }
}
