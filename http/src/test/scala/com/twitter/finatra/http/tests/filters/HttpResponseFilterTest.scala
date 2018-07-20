package com.twitter.finatra.http.tests.filters

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.HttpResponseFilter
import com.twitter.inject.Test
import com.twitter.util.{Await, Future}

class HttpResponseFilterTest extends Test {

  val respFilter = new HttpResponseFilter[Request]
  val rfc7231Regex =
    """^(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun), \d\d (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \d{4} \d\d:\d\d:\d\d GMT$"""

  val host = "www.twitter.com"

  def mkRequest: Request = {
    val request = Request()
    request.host_=(host)
    request
  }

  def mkService(location: Option[String] = None) = {
    Service.mk[Request, Response] { request =>
      val response = Response()
      response.setStatusCode(200)
      response.setContentString("test header")
      location.foreach(response.location_=)
      Future(response)
    }
  }

  def assertLocation(
    request: Request,
    service: Service[Request, Response],
    expectedLocation: Option[String]
  ) = {
    Await.result(respFilter.apply(request, service)).location should be(expectedLocation)
  }

  def checkResponse(
    request: Request,
    service: Service[Request, Response],
    expectedLocation: Option[String]
  ) = {
    try {
      assertLocation(request, service, expectedLocation)
    } finally {
      service.close()
    }
  }

  test("test response header") {
    val service = mkService()
    val request = mkRequest

    try {
      val response = Await.result(respFilter.apply(request, service))
      response.version should equal(request.version)
      response.server should equal(Some("Finatra"))
      response.contentType should equal(Some("application/octet-stream"))
      response.date.get should fullyMatch regex rfc7231Regex
    } finally {
      service.close()
    }
  }

  test("valid URIs with the http scheme should not modify the location header") {
    val location = "http://www.twitter.com"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com"))
  }

  test("valid URIs with the https scheme should not modify the location header") {
    val location = "https://www.twitter.com"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("https://www.twitter.com"))
  }

  test("URIs with the twitter scheme should not modify the location header") {
    val location = "twitter://webapp"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("twitter://webapp"))
  }

  test("absolute path URIs without a scheme should have http:// prepended in the location header") {
    val location = "/absolute_path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/absolute_path"))
  }

  test("relative path URIs without a scheme should have http:// prepended in the location header") {
    val location = "non-absolute-path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/non-absolute-path"))
  }

  test("relative path URIs starting with http without a scheme should have http:// prepended to the location header") {
    val location = "http-non-absolute-path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/http-non-absolute-path"))
  }

  test("invalid URIs causing errors should not modify the location header") {
    val location = ":/?#baduri"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some(":/?#baduri"))
  }

  test("URIs lacking a scheme should have the host prepended") {
    val location = "/test-foo:bar"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/test-foo:bar"))
  }

  test("URIs lacking a scheme but containing an authority should have http prepended") {
    val location = "//foo:bar"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://foo:bar"))
  }

  test("URIs with the mailto scheme should not modify the location header") {
    val location = "mailto:java-net@java.sun.com"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("mailto:java-net@java.sun.com"))
  }

  test("URIs with the news scheme should not modify the location header") {
    val location = "news:comp.lang.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("news:comp.lang.java"))
  }

  test("URIs with the urn scheme should not modify the location header") {
    val location = "urn:isbn:096139210x"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("urn:isbn:096139210x"))
  }

  test("Relative file paths without a scheme should have http:// prepended") {
    val location = "../../../demo/jfc/SwingSet2/src/Foo.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/../../../demo/jfc/SwingSet2/src/Foo.java"))
  }

  test("Absolute file paths without a scheme should have http:// prepended") {
    val location = "/../../../demo/jfc/SwingSet2/src/Foo.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/../../../demo/jfc/SwingSet2/src/Foo.java"))
  }

  test("Relative file paths with a scheme should not modify the location header") {
    val location = "file:///~/calendar"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("file:///~/calendar"))
  }

  test("URIs without a scheme should still include the queries and fragments in the path") {
    val location = "//www.twitter.com?query#fragment"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com?query#fragment"))
  }
}
