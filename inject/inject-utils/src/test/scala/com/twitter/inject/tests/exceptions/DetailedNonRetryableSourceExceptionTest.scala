package com.twitter.inject.tests.exceptions

import com.twitter.inject.Test
import com.twitter.inject.exceptions.DetailedNonRetryableSourcedException

class DetailedNonRetryableSourceExceptionTest extends Test {

  "DetailedNonRetryableSourcedException" should {

    "log details with no source" in {
      val e = new DetailedNonRetryableSourcedException("Non-retryable exception occurred.")
      e.toDetailsString should be(Seq(e.getClass.getSimpleName).mkString("/"))
    }

    "log details with source" in {
      val detailedNonRetryableSource = "SomeProject"
      val e = new DetailedNonRetryableSourcedException("Non-retryable exception occurred.") {
        override val source = detailedNonRetryableSource
      }

      e.toDetailsString should be(s"$detailedNonRetryableSource/" + Seq(e.getClass.getSimpleName).mkString("/"))
    }
  }
}
