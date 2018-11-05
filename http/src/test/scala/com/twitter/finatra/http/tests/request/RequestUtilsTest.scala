package com.twitter.finatra.http.tests.request

import com.twitter.finagle.http.{HeaderMap, Request}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.exceptions.{BadRequestException, NotAcceptableException}
import com.twitter.finatra.http.request.ContentType
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.inject.{Mockito, Test}
import java.net.URI

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

  test("normalizeResponseURI returns an unchanged URI for http URIs") {
    val uri = new URI("http://www.twitter.com/")
    val request = mkRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("http://www.twitter.com/")
  }

  test("normalizeResponseURI should append the host for absolute paths") {
    val uri = new URI("/absolute_path")
    val request = mkRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("http://www.twitter.com/absolute_path")
  }

  test("normalizeResponseURI rewrites URIs with no schema and no leading '/'") {
    val uri = new URI("non-absolute-path")
    val request = mkRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("http://www.twitter.com/non-absolute-path")
  }

  test("normalizeResponseURI rewrites relative path URIs that start with http") {
    val uri = new URI("http-non-absolute-path")
    val request = mkRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("http://www.twitter.com/http-non-absolute-path")
  }

  test("normalizeResponseURI prepends http to URIs missing a scheme") {
    val uri = new URI("//foo:bar")
    val request = mkRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("http://foo:bar")
  }

  test("normalizeResponseURI prepends the host when given a file path") {
    val uri = new URI("../../../demo/jfc/SwingSet2/src/Foo.java")
    val request = mkRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("http://www.twitter.com/../../../demo/jfc/SwingSet2/src/Foo.java")
  }

  test("normalizeResponseURI uses the scheme defined in the request if given") {
    val uri = new URI("http://www.twitter.com/")
    val request = mkHttpsRequest
    val normalizedURI = RequestUtils.normalizedURIWithoutScheme(uri, request)
    normalizedURI should be("https://www.twitter.com/")
  }
}
