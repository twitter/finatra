package com.twitter.finatra.tests.utils

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.finatra.http.response.ResponseUtils
import com.twitter.inject.Test

class ResponseUtilsTest extends Test {

  val internalServerErrorResponse = Response(Version.Http11, Status.InternalServerError)

  val notFoundResponse = Response(Version.Http11, Status.NotFound)
  val notFoundResponseWithBody = Response(Version.Http11, Status.NotFound)
  notFoundResponseWithBody.setContentString("not found")

  val forbiddenResponse = Response(Version.Http11, Status.Forbidden)
  val forbiddenResponseWithBody = Response(Version.Http11, Status.Forbidden)
  forbiddenResponseWithBody.setContentString("forbidden")

  val movedPermanentlyResponse = Response(Version.Http11, Status.MovedPermanently)

  val okResponse = Response(Version.Http11, Status.Ok)

  val okResponseWithBody = Response(Version.Http11, Status.Ok)
  okResponseWithBody.setContentString("ok")

  test("correctly identify 5xx response") {
    ResponseUtils.is5xxResponse(internalServerErrorResponse) should be(true)
    ResponseUtils.is5xxResponse(notFoundResponse) should be(false)
    ResponseUtils.is5xxResponse(okResponse) should be(false)
  }

  test("correctly identify 2xx response") {
    ResponseUtils.is2xxResponse(okResponse) should be(true)
    ResponseUtils.is2xxResponse(notFoundResponse) should be(false)
    ResponseUtils.is2xxResponse(internalServerErrorResponse) should be(false)
  }

  test("correct identify 4xx or 5xx responses") {
    ResponseUtils.is4xxOr5xxResponse(internalServerErrorResponse) should be(true)
    ResponseUtils.is4xxOr5xxResponse(notFoundResponse) should be(true)
    ResponseUtils.is4xxOr5xxResponse(okResponse) should be(false)
  }

  test("expect ok response") {
    intercept[java.lang.AssertionError] {
      ResponseUtils.expectOkResponse(forbiddenResponse)
    }

    ResponseUtils.expectOkResponse(okResponse)
  }

  test("expect ok response with body") {
    ResponseUtils.expectOkResponse(okResponseWithBody, "ok")
  }

  test("expect forbidden response") {
    intercept[java.lang.AssertionError] {
      ResponseUtils.expectForbiddenResponse(okResponse)
    }

    ResponseUtils.expectForbiddenResponse(forbiddenResponse)
  }

  test("expect forbidden response with body") {
    ResponseUtils.expectForbiddenResponse(forbiddenResponseWithBody, "forbidden")
  }

  test("expect not found response") {
    intercept[java.lang.AssertionError] {
      ResponseUtils.expectNotFoundResponse(okResponse)
    }

    ResponseUtils.expectNotFoundResponse(notFoundResponse)
  }

  test("expect not found response with body") {
    ResponseUtils.expectNotFoundResponse(notFoundResponseWithBody, "not found")
  }
}
