package com.twitter.finatra.tests.utils

import com.twitter.finagle.http.{Version, Response}
import com.twitter.finatra.utils.ResponseUtils
import com.twitter.inject.Test
import org.jboss.netty.handler.codec.http.{HttpResponseStatus, DefaultHttpResponse}

class ResponseUtilsTest
  extends Test {

  val internalServerErrorResponse = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.INTERNAL_SERVER_ERROR))

  val notFoundResponse = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.NOT_FOUND))
  val notFoundResponseWithBody = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.NOT_FOUND))
  notFoundResponseWithBody.setContentString("not found")

  val forbiddenResponse = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.FORBIDDEN))
  val forbiddenResponseWithBody = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.FORBIDDEN))
  forbiddenResponseWithBody.setContentString("forbidden")

  val movedPermanentlyResponse = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.MOVED_PERMANENTLY))

  val okResponse = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.OK))
  val okResponseWithBody = Response(
    new DefaultHttpResponse(
      Version.Http11,
      HttpResponseStatus.OK))
  okResponseWithBody.setContentString("ok")

  "ResponseUtils" should {

    "correctly identify 5xx response" in {
      ResponseUtils.is5xxResponse(internalServerErrorResponse) should be(true)
      ResponseUtils.is5xxResponse(notFoundResponse) should be(false)
      ResponseUtils.is5xxResponse(okResponse) should be(false)
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
