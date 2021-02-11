package com.twitter.finatra.http.tests.conversions

import com.twitter.finatra.http.conversions.optionHttp._
import com.twitter.finatra.http.exceptions._
import com.twitter.inject.Test
import com.twitter.util.{Future, Throw, Try}

class OptionHttpConversionsTest extends Test {

  test("Option[T]#valueOrNotFound when Some") {
    Some(1).valueOrNotFound("foo") should equal(1)
  }

  test("Option[T]#valueOrNotFound when None") {
    val e = intercept[NotFoundException] {
      None.valueOrNotFound("foo")
    }
    e.errors should equal(Seq("foo"))
  }

  test("Option[T]#toTryOrServerError when Some") {
    Some(1).toTryOrServerError("foo") should equal(Try(1))
  }

  test("Option[T]#toTryOrServerError when None") {
    None.toTryOrServerError("foo") should equal(Throw(InternalServerErrorException("foo")))
  }

  test("Option[T]#toFutureOrNotFound when Some") {
    assertFuture(Some(1).toFutureOrNotFound(), Future(1))
  }

  test("Option[T]#toFutureOrBadRequest when Some") {
    assertFuture(Some(1).toFutureOrBadRequest(), Future(1))
  }

  test("Option[T]#toFutureOrServiceError when Some") {
    assertFuture(Some(1).toFutureOrServerError(), Future(1))
  }

  test("Option[T]#toFutureOrForbidden when Some") {
    assertFuture(Some(1).toFutureOrForbidden(), Future(1))
  }

  test("Option[T]#toFutureOrNotFound when None") {
    assertFailedFuture[NotFoundException](None.toFutureOrNotFound())
  }

  test("Option[T]#toFutureOrBadRequest when None") {
    assertFailedFuture[BadRequestException](None.toFutureOrBadRequest())
  }

  test("Option[T]#toFutureOrServiceError when None") {
    assertFailedFuture[InternalServerErrorException](None.toFutureOrServerError())
  }

  test("Option[T]#toFutureOrForbidden when None") {
    assertFailedFuture[ForbiddenException](None.toFutureOrForbidden())
  }
}
