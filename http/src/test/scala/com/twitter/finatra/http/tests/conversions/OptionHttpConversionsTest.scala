package com.twitter.finatra.http.tests.conversions

import com.twitter.finatra.http.conversions.optionHttp._
import com.twitter.finatra.http.exceptions._
import com.twitter.inject.Test
import com.twitter.util.{Future, Throw, Try}

class OptionHttpConversionsTest extends Test {

  "Option[T]" should {

    "#valueOrNotFound when Some" in {
      Some(1).valueOrNotFound("foo") should equal(1)
    }

    "#valueOrNotFound when None" in {
      val e = intercept[NotFoundException] {
        None.valueOrNotFound("foo")
      }
      e.errors should equal(Seq("foo"))
    }

    "#toTryOrServerError when Some" in {
      Some(1).toTryOrServerError("foo") should equal(Try(1))
    }

    "#toTryOrServerError when None" in {
      None.toTryOrServerError("foo") should equal(Throw(InternalServerErrorException("foo")))
    }

    "#toFutureOrNotFound when Some" in {
      assertFuture(
        Some(1).toFutureOrNotFound(),
        Future(1))
    }

    "#toFutureOrBadRequest when Some" in {
      assertFuture(
        Some(1).toFutureOrBadRequest(),
        Future(1))
    }

    "#toFutureOrServiceError when Some" in {
      assertFuture(
        Some(1).toFutureOrServerError(),
        Future(1))
    }

    "#toFutureOrForbidden when Some" in {
      assertFuture(
        Some(1).toFutureOrForbidden(),
        Future(1))
    }

    "#toFutureOrNotFound when None" in {
      assertFailedFuture[NotFoundException](
        None.toFutureOrNotFound())
    }

    "#toFutureOrBadRequest when None" in {
      assertFailedFuture[BadRequestException](
        None.toFutureOrBadRequest())
    }

    "#toFutureOrServiceError when None" in {
      assertFailedFuture[InternalServerErrorException](
        None.toFutureOrServerError())
    }

    "#toFutureOrForbidden when None" in {
      assertFailedFuture[ForbiddenException](
        None.toFutureOrForbidden())
    }
  }
}
