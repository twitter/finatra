package com.twitter.finatra.http.tests.request

import com.twitter.finagle.http.{MapHeaderMap, Request}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.exceptions.{BadRequestException, NotAcceptableException}
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.finatra.request.ContentType
import com.twitter.inject.{Mockito, Test}

class RequestUtilsTest
  extends Test
  with Mockito {

  "RequestUtils" should {

    "throw BadRequestException when missing host header for pathUrl" in {
      val request = smartMock[Request]
      request.headerMap returns MapHeaderMap()
      request.host returns None

      intercept[BadRequestException] {
        RequestUtils.pathUrl(request)
      }
    }

    "pathUrl with slash" in {
      val request = smartMock[Request]
      request.headerMap returns MapHeaderMap()
      request.host returns Some("www.foo.com")
      request.path returns "/part/next/end/"

      val result = RequestUtils.pathUrl(request)
      result should equal("http://www.foo.com/part/next/end/")
    }

    "respondTo */* content type in request when no accept header is sent" in {
      val request = smartMock[Request]
      request.headerMap returns MapHeaderMap()

       val response = RequestUtils.respondTo(request) {
        case _ => true
      }

      response should be(true)
    }

    "respondTo text/html content-type in request" in {
      val request = smartMock[Request]
      request.headerMap returns MapHeaderMap(HttpHeaders.Accept -> ContentType.HTML.toString)

      val response = RequestUtils.respondTo(request) {
        case ContentType.HTML => true
        case _ => false
      }

      response should be(true)
    }

    "respondTo application/json content-type in request" in {
      val request = smartMock[Request]
      request.headerMap returns MapHeaderMap(HttpHeaders.Accept -> ContentType.JSON.toString)

      val response = RequestUtils.respondTo(request) {
        case ContentType.JSON => true
        case _ => false
      }

      response should be(true)
    }

    "return NotAcceptableException for request" in {
      val request = smartMock[Request]
      // accept application/json
      request.headerMap returns MapHeaderMap(HttpHeaders.Accept -> ContentType.JSON.toString)

      intercept[NotAcceptableException] {
        // only handle text/html
        RequestUtils.respondTo(request) {
          case ContentType.HTML => true
        }
      }
    }
  }

}
