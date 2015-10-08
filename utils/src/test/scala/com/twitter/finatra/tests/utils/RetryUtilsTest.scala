package com.twitter.finatra.tests.utils

import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.utils.{RetryPolicyUtils, RetryUtils}
import com.twitter.inject.Test
import com.twitter.util.{Await, Future}

class RetryUtilsTest extends Test {

  val nonFatalExponentialPolicy = RetryPolicyUtils.exponentialRetry(
    start = 10.millis,
    multiplier = 2,
    numRetries = 4,
    shouldRetry = RetryPolicyUtils.NonFatalExceptions)

  val constantHttpSuccessPolicy = RetryPolicyUtils.constantRetry(
    start = 10.millis,
    numRetries = 4,
    shouldRetry = RetryPolicyUtils.Http4xxOr5xxResponses)

  "retry with futures succeeds" in {
    var numRuns = 0

    val result = RetryUtils.retryFuture(nonFatalExponentialPolicy) {
      numRuns += 1
      if (numRuns == 3)
        Future(26)
      else
        throw new RuntimeException("fake failure")
    }

    Await.result(result) should be(26)
  }

  "retry with futures fails" in {
    val result = RetryUtils.retryFuture(nonFatalExponentialPolicy) {
      throw new IllegalArgumentException("foo")
    }

    intercept[IllegalArgumentException] {
      Await.result(result)
    }
  }

  "HTTP retry with futures succeeds" in {
    var numRuns = 0

    val result = RetryUtils.retryFuture(constantHttpSuccessPolicy) {
      numRuns += 1
      if (numRuns == 1)
        Future(Response(Status.InternalServerError))
      else if (numRuns == 2)
        Future(Response(Status.NotFound))
      else if (numRuns == 3)
        Future(Response(Status.Ok))
      else
        fail("shouldn't get here")
    }

    Await.result(result).status should be(Status.Ok)
  }

  "HTTP retry with futures fails" in {
    val result = RetryUtils.retryFuture(constantHttpSuccessPolicy) {
      Future(Response(Status.NotFound))
    }

    Await.result(result).status should be(Status.NotFound)
  }

  "retry non future" in {
    var numRuns = 0

    val result = RetryUtils.retry(nonFatalExponentialPolicy) {
      numRuns += 1
      if (numRuns == 3)
        26
      else
        throw new RuntimeException("fake failure")
    }

    result.get should be(26)
  }
}
