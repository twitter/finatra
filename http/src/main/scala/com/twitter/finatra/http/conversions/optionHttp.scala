package com.twitter.finatra.http.conversions

import com.twitter.finatra.conversions.option.RichOption
import com.twitter.finatra.http.exceptions._

object optionHttp {

  /* -------------------------------------------------------- */
  implicit class HttpRichOption[A](val self: Option[A]) extends AnyVal {
    def valueOrNotFound(msg: String = ""): A = {
      self.getOrElse(throw new NotFoundException(msg))
    }

    def toFutureOrNotFound(msg: String = "") = {
      RichOption.toFutureOrFail(self, NotFoundException(msg))
    }

    def toFutureOrBadRequest(msg: String = "") = {
      RichOption.toFutureOrFail(self, BadRequestException(msg))
    }

    def toFutureOrServerError(msg: String = "") = {
      RichOption.toFutureOrFail(self, InternalServerErrorException(msg))
    }

    def toFutureOrForbidden(msg: String = "") = {
      RichOption.toFutureOrFail(self, ForbiddenException(msg))
    }

    def toTryOrServerError(msg: String = "") = {
      RichOption.toTryOrFail(self, InternalServerErrorException(msg))
    }
  }
}
