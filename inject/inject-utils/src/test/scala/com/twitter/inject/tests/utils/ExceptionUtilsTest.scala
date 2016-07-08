package com.twitter.inject.tests.utils

import com.twitter.conversions.time._
import com.twitter.finagle.{FailedFastException, IndividualRequestTimeoutException}
import com.twitter.inject.Test
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.util.{Throw, Return}

class ExceptionUtilsTest extends Test {

  "ExceptionUtils" should {

    "toExceptionDetails" in {
      val details = ExceptionUtils.toExceptionDetails(new FailedFastException("This happened quick"))
      println(details)
    }

    "toExceptionMessage on Return" in {
      ExceptionUtils.toExceptionMessage(Return("Success")) should be("")
    }

    "toExceptionMessage on FailedFastException" in {
      val exception = new FailedFastException("This happened quick")
      ExceptionUtils.toExceptionMessage(Throw(exception)) should be(exception.getClass.getName)
    }

    "toDetailedExceptionMessage on FailedFastException" in {
      val exception = new FailedFastException("This happened quick")
      ExceptionUtils.toDetailedExceptionMessage(Throw(exception)) should be(exception.getClass.getName + " " + exception.getClass.getName)
    }

    "toExceptionMessage with no message" in {
      val exception = new Exception()
      ExceptionUtils.toExceptionMessage(exception) should be(exception.getClass.getName)
    }

    "strip new lines" in {
      ExceptionUtils.stripNewlines(new IndividualRequestTimeoutException(1.second)) should not contain "\n"
    }
  }

}
