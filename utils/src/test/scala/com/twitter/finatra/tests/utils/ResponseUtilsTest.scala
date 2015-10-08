package com.twitter.finatra.tests.utils

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.finatra.utils.ResponseUtils
import com.twitter.inject.Test

class ResponseUtilsTest
  extends Test {

  val internalServerErrorResponse = Response(
      Version.Http11,
      Status.InternalServerError)

  val notFoundResponse = Response(
      Version.Http11,
      Status.NotFound)
  val notFoundResponseWithBody = Response(
      Version.Http11,
      Status.NotFound)
  notFoundResponseWithBody.setContentString("not found")

  val forbiddenResponse = Response(
      Version.Http11,
      Status.Forbidden)
  val forbiddenResponseWithBody = Response(
      Version.Http11,
      Status.Forbidden)
  forbiddenResponseWithBody.setContentString("forbidden")

  val movedPermanentlyResponse = Response(
      Version.Http11,
      Status.MovedPermanently)

  val okResponse = Response(
      Version.Http11,
      Status.Ok)

  val okResponseWithBody = Response(
      Version.Http11,
      Status.Ok)
  okResponseWithBody.setContentString("ok")

  "ResponseUtils" should {

    "correctly identify 5xx response" in {
      ResponseUtils.is5xxResponse(internalServerErrorResponse) should be(true)
      ResponseUtils.is5xxResponse(notFoundResponse) should be(false)
      ResponseUtils.is5xxResponse(okResponse) should be(false)
    }

    "correctly identify 2xx response" in {
      ResponseUtils.is2xxResponse(okResponse) should be(true)
      ResponseUtils.is2xxResponse(notFoundResponse) should be(false)
      ResponseUtils.is2xxResponse(internalServerErrorResponse) should be(false)
    }

    "correct identify 4xx or 5xx responses" in {
      ResponseUtils.is4xxOr5xxResponse(internalServerErrorResponse) should be(true)
      ResponseUtils.is4xxOr5xxResponse(notFoundResponse) should be(true)
      ResponseUtils.is4xxOr5xxResponse(okResponse) should be(false)
    }

    "expect ok response" in {
      intercept[java.lang.AssertionError] {
        ResponseUtils.expectOkResponse(forbiddenResponse)
      }

      ResponseUtils.expectOkResponse(okResponse)
    }

    "expect ok response with body" in {
      ResponseUtils.expectOkResponse(okResponseWithBody, "ok")
    }

    "expect forbidden response" in {
      intercept[java.lang.AssertionError] {
        ResponseUtils.expectForbiddenResponse(okResponse)
      }

      ResponseUtils.expectForbiddenResponse(forbiddenResponse)
    }

    "expect forbidden response with body" in {
      ResponseUtils.expectForbiddenResponse(forbiddenResponseWithBody, "forbidden")
    }

    "expect not found response" in {
        intercept[java.lang.AssertionError] {
        ResponseUtils.expectNotFoundResponse(okResponse)
      }

      ResponseUtils.expectNotFoundResponse(notFoundResponse)
    }

    "expect not found response with body" in {
      ResponseUtils.expectNotFoundResponse(notFoundResponseWithBody, "not found")
    }
  }
}
