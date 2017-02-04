package com.twitter.finatra.tests.utils

import com.twitter.finagle.http.{Status, Response}
import com.twitter.finatra.utils.ResponseUtils
import com.twitter.inject.Test
import com.twitter.inject.conversions.time._
import com.twitter.inject.utils.{RetryPolicyUtils, RetryUtils}
import com.twitter.util.{Future, Await}

class HttpRetryUtilsTest extends Test {

  val constantHttpSuccessPolicy = RetryPolicyUtils.constantRetry(
    start = 10.millis,
    numRetries = 4,
    shouldRetry = ResponseUtils.Http4xxOr5xxResponses)

  test("HTTP retry#with futures succeeds") {
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

  test("HTTP retry#with futures fails") {
    val result = RetryUtils.retryFuture(constantHttpSuccessPolicy) {
      Future(Response(Status.NotFound))
    }

    Await.result(result).status should be(Status.NotFound)
  }

}
