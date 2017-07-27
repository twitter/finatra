package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.option._
import com.twitter.util.{Future, Throw, Try}

class OptionsConversionsTest extends Test {

  test("RichOption#toFutureOrFail when Some") {
    assertFuture(Some(1).toFutureOrFail(TestException), Future(1))
  }
  test("RichOption#toFutureOrFail when None") {
    assertFailedFuture[TestException](None.toFutureOrFail(TestException))
  }
  test("RichOption#toTryOrFail when Some") {
    Some(1).toTryOrFail(TestException) should equal(Try(1))
  }
  test("RichOption#toTryOrFail when None") {
    None.toTryOrFail(TestException) should equal(Throw(TestException))
  }
  test("RichOption#toFutureOrElse when Some") {
    assertFuture(Some(1).toFutureOrElse(2), Future(1))
  }
  test("RichOption#toFutureOrElse when None") {
    val noneInt: Option[Int] = None
    assertFuture(noneInt.toFutureOrElse(2), Future(2))
  }
  test("RichOption#toFutureOrElse with Future when Some") {
    assertFuture(Some(1).toFutureOrElse(Future(2)), Future(1))
  }
  test("RichOption#toFutureOrElse with Future when None") {
    val noneInt: Option[Int] = None
    assertFuture(noneInt.toFutureOrElse(Future(2)), Future(2))
  }

  test("RichOptionFuture#toFutureOption when Some") {
    assertFuture(Some(Future(1)).toFutureOption, Future(Some(1)))
  }
  test("RichOptionFuture#toFutureOption when None") {
    assertFuture(None.toFutureOption, Future(None))
  }

  test("format#return a formatted string when Some") {
    Some(1).format("The value is %d") should equal("The value is 1")
  }
  test("format#return empty string when None") {
    None.format("The value is %i") should equal("")
  }

  test("option map#map inner values when some") {
    Some(Map("a" -> 1)) mapInnerValues { _.toString } should equal(Some(Map("a" -> "1")))
  }

  test("option map#map inner values when none") {
    Option.empty[Map[String, Int]] mapInnerValues { _.toString } should equal(None)
  }

  object TestException extends TestException

  class TestException extends Exception

}
