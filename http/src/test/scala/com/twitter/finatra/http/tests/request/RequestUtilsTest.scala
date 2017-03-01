package com.twitter.finatra.http.tests.request

import com.twitter.finagle.http.{HeaderMap, Request}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.exceptions.{BadRequestException, NotAcceptableException}
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.finatra.request.ContentType
import com.twitter.inject.{Mockito, Test}

class RequestUtilsTest extends Test with Mockito {

  test("throw BadRequestException when missing host header for pathUrl") {
    val request = smartMock[Request]
    request.headerMap returns HeaderMap()
    request.host returns None

    intercept[BadRequestException] {
      RequestUtils.pathUrl(request)
    }
  }

  test("pathUrl with slash") {
    val request = smartMock[Request]
    request.headerMap returns HeaderMap()
    request.host returns Some("www.foo.com")
    request.path returns "/part/next/end/"

    val result = RequestUtils.pathUrl(request)
    result should equal("http://www.foo.com/part/next/end/")
  }

  test("respondTo */* content type in request when no accept header is sent") {
    val request = smartMock[Request]
    request.headerMap returns HeaderMap()

    val response = RequestUtils.respondTo(request) {
      case _ => true
    }

    response should be(true)
  }

  test("respondTo text/html content-type in request") {
    val request = smartMock[Request]
    request.headerMap returns HeaderMap(HttpHeaders.Accept -> ContentType.HTML.toString)

    val response = RequestUtils.respondTo(request) {
      case ContentType.HTML => true
      case _ => false
    }

    response should be(true)
  }

  test("respondTo application/json content-type in request") {
    val request = smartMock[Request]
    request.headerMap returns HeaderMap(HttpHeaders.Accept -> ContentType.JSON.toString)

    val response = RequestUtils.respondTo(request) {
      case ContentType.JSON => true
      case _ => false
    }

    response should be(true)
  }

  test("return NotAcceptableException for request") {
    val request = smartMock[Request]
    // accept application/json
    request.headerMap returns HeaderMap(HttpHeaders.Accept -> ContentType.JSON.toString)

    intercept[NotAcceptableException] {
      // only handle text/html
      RequestUtils.respondTo(request) {
        case ContentType.HTML => true
      }
    }
  }

}
