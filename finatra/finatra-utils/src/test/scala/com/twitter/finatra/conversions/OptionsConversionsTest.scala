package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.options._
import com.twitter.finatra.test.Test
import com.twitter.util.Future

class OptionsConversionsTest extends Test {

  "RichOption" should {
    "#toFutureOrFail when Some" in {
      assertFuture(
        Some(1).toFutureOrFail(TestException),
        Future(1))
    }
    "#toFutureOrFail when None" in {
      assertFailedFuture[TestException](
        None.toFutureOrFail(TestException))
    }
    "#toFutureOrFallback when Some" in {
      assertFuture(
        Some(1).toFutureWithFallback(2),
        Future(1))
    }
    "#toFutureOrFallback when None" in {
      val noneInt: Option[Int] = None
      assertFuture(
        noneInt.toFutureWithFallback(2),
        Future(2))
    }
  }

  "RichOptionFuture" should {
    "#toFutureOption when Some" in {
      assertFuture(
        Some(Future(1)).toFutureOption,
        Future(Some(1)))
    }
    "#toFutureOption when None" in {
      assertFuture(
        None.toFutureOption,
        Future(None))
    }
  }

  "format" should {
    "return a formatted string when Some" in {
      Some(1).format("The value is %d") should equal("The value is 1")
    }
    "return empty string when None" in {
      None.format("The value is %i") should equal("")
    }
  }
}
