package com.twitter.finatra.http.tests.request

import com.twitter.finagle.http.{Fields, Request}
import com.twitter.finatra.http.exceptions.{BadRequestException, NotAcceptableException}
import com.twitter.finatra.http.request.{ContentType, RequestUtils}
import com.twitter.inject.Test
import com.twitter.util.mock.Mockito

class RequestUtilsTest extends Test with Mockito {
  val host = "www.twitter.com"

  def mkRequest: Request = {
    val request = Request()
    request.host_=(host)
    request
  }

  def mkHttpsRequest: Request = {
    val request = mkRequest
    request.headerMap.set("x-forwarded-proto", "https")
    request
  }

  test("throw BadRequestException when missing host header for pathUrl") {
    val request = Request()

    intercept[BadRequestException] {
      RequestUtils.pathUrl(request)
    }
  }

  test("pathUrl with slash") {
    val request = Request("/part/next/end/")
    request.host = "www.foo.com"

    val result = RequestUtils.pathUrl(request)
    result should equal("http://www.foo.com/part/next/end/")
  }

  test("respondTo */* content type in request when no accept header is sent") {
    val request = Request()

    val response = RequestUtils.respondTo(request) {
      case _ => true
    }

    response should be(true)
  }

  test("respondTo text/html content-type in request") {
    val request = Request()
    request.headerMap.add(Fields.Accept, ContentType.HTML.toString)

    val response = RequestUtils.respondTo(request) {
      case ContentType.HTML => true
      case _ => false
    }

    response should be(true)
  }

  test("respondTo application/json content-type in request") {
    val request = Request()
    request.headerMap.add(Fields.Accept, ContentType.JSON.toString)

    val response = RequestUtils.respondTo(request) {
      case ContentType.JSON => true
      case _ => false
    }

    response should be(true)
  }

  test("return NotAcceptableException for request") {
    val request = Request()
    // accept application/json
    request.headerMap.add(Fields.Accept, ContentType.JSON.toString)

    intercept[NotAcceptableException] {
      // only handle text/html
      RequestUtils.respondTo(request) {
        case ContentType.HTML => true
      }
    }
  }
}
