package com.twitter.finatra.http.conversions

import com.twitter.finatra.http.conversions.optionHttp._
import com.twitter.finatra.http.exceptions._
import com.twitter.inject.Test
import com.twitter.util.Future

class OptionHttpConversionsTest extends Test {

  "Option[T]" should {
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
