package com.twitter.finatra.httpclient

import com.twitter.inject.Test
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._

class RequestBuilderTest extends Test {

  "get" in {
    val request = RequestBuilder.get("/abc")
      .header("c", "3")
      .headers(
        "a" -> "1",
        "b" -> "2")

    request.uri should be("/abc")
    request.method should be(GET)

    request.headerMap should be(Map(
      "a" -> "1",
      "b" -> "2",
      "c" -> "3"))
  }

  "post" in {
    val request = RequestBuilder.post("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(POST, request)
  }

  "put" in {
    val request = RequestBuilder.put("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(PUT, request)
  }

  "delete" in {
    val request = RequestBuilder.delete("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(DELETE, request)
  }

  "head" in {
    val request = RequestBuilder.head("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(HEAD, request)
  }

  "post json" in {
    val request = RequestBuilder.post("/abc")
      .body("{}")

    request.headerMap("Content-Type") should be("application/json;charset=utf-8")
    request.contentString should be("{}")
  }

  def assertRequestWithBody(expectedMethod: HttpMethod, request: RequestBuilder): Unit = {
    request.uri should be("/abc")
    request.method should be(expectedMethod)

    request.headerMap should be(Map(
      "a" -> "1",
      "b" -> "2",
      "c" -> "3",
      "Content-Length" -> "8",
      "Content-Type" -> "foo/bar"))

    request.contentString should be("testbody")
  }
}
