package com.twitter.finatra.httpclient

import com.twitter.finagle.http.{Message, Method}
import com.twitter.finagle.http.Method._
import com.twitter.inject.Test

class RequestBuilderTest extends Test {

  "get" in {
    val request = RequestBuilder.get("/abc")
      .header("c", "3")
      .headers(
        "a" -> "1",
        "b" -> "2")

    request.uri should be("/abc")
    request.method should be(Get)

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

    assertRequestWithBody(Post, request)
  }

  "put" in {
    val request = RequestBuilder.put("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Put, request)
  }

  "put with form params" in {
    val request = RequestBuilder.put("/abc")
      .body("Pcode=9999&Locality=A%20New%20Location", Message.ContentTypeWwwFrom)
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

      request.uri should be("/abc")
      request.method should be(Put)

      request.headerMap should be(Map(
        "a" -> "1",
        "b" -> "2",
        "c" -> "3",
        "Content-Length" -> "38",
        "Content-Type" -> Message.ContentTypeWwwFrom))

      request.contentString should be("Pcode=9999&Locality=A%20New%20Location")
  }

  "patch" in {
    val request = RequestBuilder.patch("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Patch, request)
  }

  "delete" in {
    val request = RequestBuilder.delete("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Delete, request)
  }

  "head" in {
    val request = RequestBuilder.head("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Head, request)
  }

  "trace" in {
    val request = RequestBuilder.trace("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Trace, request)
  }

  "connect" in {
    val request = RequestBuilder.connect("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Connect, request)
  }

  "options" in {
    val request = RequestBuilder.options("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers(
        "a" -> "1",
        "b" -> "2")

    assertRequestWithBody(Options, request)
  }

  "post json" in {
    val request = RequestBuilder.post("/abc")
      .body("{}")

    request.headerMap("Content-Type") should be("application/json;charset=utf-8")
    request.contentString should be("{}")
  }

  "post utf8 content" in {
    val request = RequestBuilder.post("/abc")
      .body("ＴＥＳＴＢＯＤＹ")

    request.headerMap("Content-Length") should be("24")
  }

  def assertRequestWithBody(expectedMethod: Method, request: RequestBuilder): Unit = {
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
