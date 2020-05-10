package com.twitter.inject.tests.utils

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{FailedFastException, IndividualRequestTimeoutException}
import com.twitter.inject.Test
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.util.{Throw, Return}

class ExceptionUtilsTest extends Test {

  test("ExceptionUtils#toExceptionDetails") {
    val details = ExceptionUtils.toExceptionDetails(new FailedFastException("This happened quick"))
    println(details)
  }

  test("ExceptionUtils#toExceptionMessage on Return") {
    ExceptionUtils.toExceptionMessage(Return("Success")) should be("")
  }

  test("ExceptionUtils#toExceptionMessage on FailedFastException") {
    val exception = new FailedFastException("This happened quick")
    ExceptionUtils.toExceptionMessage(Throw(exception)) should be(exception.getClass.getName)
  }

  test("ExceptionUtils#toDetailedExceptionMessage on FailedFastException") {
    val exception = new FailedFastException("This happened quick")
    ExceptionUtils.toDetailedExceptionMessage(Throw(exception)) should be(
      exception.getClass.getName + " " + exception.getClass.getName)
  }

  test("ExceptionUtils#toExceptionMessage with no message") {
    val exception = new Exception()
    ExceptionUtils.toExceptionMessage(exception) should be(exception.getClass.getName)
  }

  test("ExceptionUtils#strip new lines") {
    ExceptionUtils.stripNewlines(
      new IndividualRequestTimeoutException(1.second)) should not contain "\n"
  }

}
