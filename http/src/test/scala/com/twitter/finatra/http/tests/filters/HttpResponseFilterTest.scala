package com.twitter.finatra.http.tests.filters

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Fields, Request, Response}
import com.twitter.finatra.http.filters.HttpResponseFilter
import com.twitter.inject.Test
import com.twitter.util.Future
import java.net.URLEncoder
import org.scalatest.Assertion

abstract class AbstractHttpResponseFilterTest extends Test {

  protected def httpResponseFilter: HttpResponseFilter[Request]

  private[this] val rfc7231Regex =
    """^(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun), \d\d (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \d{4} \d\d:\d\d:\d\d GMT$"""

  private[this] val host = "www.twitter.com"

  test("HttpResponseFilter#test response header") {
    val service = mkService()
    val request = mkRequest

    try {
      val response = await(httpResponseFilter.apply(request, service))
      response.version should equal(request.version)
      response.server should equal(Some("Finatra"))
      response.contentType should equal(Some("application/octet-stream"))
      response.date.get should fullyMatch regex rfc7231Regex
    } finally {
      service.close()
    }
  }

  test("HttpResponseFilter#valid URIs with the http scheme should not modify the location header") {
    val location = "http://www.twitter.com"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com"))
  }

  test(
    "HttpResponseFilter#valid URIs with the https scheme should not modify the location header") {
    val location = "https://www.twitter.com"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("https://www.twitter.com"))
  }

  test("HttpResponseFilter#URIs with the twitter scheme should not modify the location header") {
    val location = "twitter://webapp"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("twitter://webapp"))
  }

  test("HttpResponseFilter#invalid URIs causing errors should not modify the location header") {
    val location = ":/?#baduri"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some(":/?#baduri"))
  }

  test("HttpResponseFilter#URIs with the mailto scheme should not modify the location header") {
    val location = "mailto:java-net@java.sun.com"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("mailto:java-net@java.sun.com"))
  }

  test("HttpResponseFilter#URIs with the news scheme should not modify the location header") {
    val location = "news:comp.lang.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("news:comp.lang.java"))
  }

  test("HttpResponseFilter#URIs with the urn scheme should not modify the location header") {
    val location = "urn:isbn:096139210x"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("urn:isbn:096139210x"))
  }

  test(
    "HttpResponseFilter#relative file paths with a scheme should not modify the location header") {
    val location = "file:///~/calendar"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("file:///~/calendar"))
  }

  test("HttpResponseFilter#will not change invalid header 1") {
    val encodedCRLF = URLEncoder.encode("\r\ninvalid: header", "UTF-8")
    val location = s"foo.com/next?what=${encodedCRLF}"
    val svc = httpResponseFilter.andThen(Service.mk { _: Request =>
      val resp = Response()
      resp.location = location
      Future.value(resp)
    })

    val request = mkRequest
    // logs a warning
    checkResponse(request, svc, Some(location))
  }

  test("HttpResponseFilter#will not change invalid header 2") {
    // en dash
    val encoded = URLEncoder.encode("mobile\u2013app#link", "UTF-8")
    val location = s"next?what=${encoded}"
    val svc = httpResponseFilter.andThen(Service.mk { _: Request =>
      val resp = Response()
      resp.location = location
      Future.value(resp)
    })

    val request = mkRequest
    // logs a warning
    checkResponse(request, svc, Some(location))
  }

  test("HttpResponseFilter#uses the scheme defined in the request if given") {
    val location = "http://www.twitter.com/"
    val request = mkHttpsRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("https://www.twitter.com/"))
  }

  protected def mkRequest: Request = {
    val request = Request()
    request.host = host
    request
  }

  def mkHttpsRequest: Request = {
    val request = mkRequest
    request.headerMap.set("x-forwarded-proto", "https")
    request
  }

  protected def mkService(location: Option[String] = None): Service[Request, Response] = {
    Service.mk[Request, Response] { _ =>
      val response = Response().statusCode(200)
      response.setContentString(this.getClass.getSimpleName)
      // for testing we set the location header unsafely to allow for
      // testing unsafe values with the HttpResponseFilter
      location.foreach(value => response.headerMap.setUnsafe(Fields.Location, value))
      Future(response)
    }
  }

  private[this] def assertLocation(
    request: Request,
    service: Service[Request, Response],
    expected: Option[String]
  ): Assertion = {
    val actual = await(httpResponseFilter.apply(request, service)).location
    actual should equal(expected)
  }

  protected def checkResponse(
    request: Request,
    service: Service[Request, Response],
    expectedLocation: Option[String]
  ): Assertion = {
    try {
      assertLocation(request, service, expectedLocation)
    } finally {
      service.close()
    }
  }
}

class FullyQualifyLocationHeaderHttpResponseFilterTest extends AbstractHttpResponseFilterTest {
  override val httpResponseFilter: HttpResponseFilter[Request] =
    new HttpResponseFilter[Request](fullyQualifyLocationHeader = true)

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

  test(
    "URIs without a scheme should still include the queries and fragments in the path and have http prepended") {
    val location = "//www.twitter.com?query#fragment"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com?query#fragment"))
  }

  test("absolute path URIs without a scheme should have http:// prepended in the location header") {
    val location = "/absolute_path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/absolute_path"))
  }

  test("relative path URIs without a scheme should have http:// prepended in the location header") {
    val location = "relative-path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/relative-path"))
  }

  test(
    "relative path URIs starting with http without a scheme should have http:// prepended to the location header") {
    val location = "http-relative-path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/http-relative-path"))
  }

  test("Relative file paths without a scheme should have http:// prepended") {
    val location = "../../../demo/jfc/SwingSet2/src/Foo.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(
      request,
      service,
      Some("http://www.twitter.com/../../../demo/jfc/SwingSet2/src/Foo.java"))
  }

  test("Absolute file paths without a scheme should get normalized and have http:// prepended") {
    val location = "/a/b/c/./../../g"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("http://www.twitter.com/a/g"))
  }

  test("Absolute file paths without a scheme should have http:// prepended") {
    val location = "/../../../demo/jfc/SwingSet2/src/Foo.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(
      request,
      service,
      Some("http://www.twitter.com/../../../demo/jfc/SwingSet2/src/Foo.java"))
  }
}

class DefaultHttpResponseFilterTest extends AbstractHttpResponseFilterTest {
  override val httpResponseFilter: HttpResponseFilter[Request] =
    new HttpResponseFilter[Request] // fullyQualifyLocationHeader = false

  test("URIs lacking a scheme should not have the host prepended") {
    val location = "/test-foo:bar"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/test-foo:bar"))
  }

  test("URIs lacking a scheme but containing an authority should not have http prepended") {
    val location = "//foo:bar"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("//foo:bar"))
  }

  test("URIs without a scheme should still include the queries and fragments in the path") {
    val location = "//www.twitter.com?query#fragment"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("//www.twitter.com?query#fragment"))
  }

  test(
    "absolute path URIs without a scheme should not have http:// prepended in the location header") {
    val location = "/absolute_path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/absolute_path"))
  }

  test(
    "relative path URIs without a scheme should not have http:// prepended in the location header") {
    val location = "relative-path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/relative-path"))
  }

  test(
    "relative path URIs starting with http without a scheme should not have http:// prepended to the location header") {
    val location = "http-relative-path"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/http-relative-path"))
  }

  test("Relative file paths without a scheme should not have http:// prepended") {
    val location = "../../../demo/jfc/SwingSet2/src/Foo.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/../../../demo/jfc/SwingSet2/src/Foo.java"))
  }

  test(
    "Absolute file paths without a scheme should get normalized and not have http:// prepended") {
    val location = "/a/b/c/./../../g"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/a/g"))
  }

  test("Absolute file paths without a scheme should not have http:// prepended") {
    val location = "/../../../demo/jfc/SwingSet2/src/Foo.java"
    val request = mkRequest
    val service = mkService(Some(location))
    checkResponse(request, service, Some("/../../../demo/jfc/SwingSet2/src/Foo.java"))
  }
}
