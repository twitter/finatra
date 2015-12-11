package com.twitter.finatra.httpclient

import com.twitter.finagle.http.Method
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
