package com.twitter.inject.tests.utils

import com.twitter.inject.Test
import com.twitter.inject.conversions.time._
import com.twitter.inject.utils.{RetryPolicyUtils, RetryUtils}
import com.twitter.util.{Await, Future}

class RetryUtilsTest extends Test {

  val nonFatalExponentialPolicy = RetryPolicyUtils.exponentialRetry(
    start = 10.millis,
    multiplier = 2,
    numRetries = 4,
    shouldRetry = RetryPolicyUtils.NonFatalExceptions)

  test("Retry#futures succeeds") {
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

  test("Retry#non future") {
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
