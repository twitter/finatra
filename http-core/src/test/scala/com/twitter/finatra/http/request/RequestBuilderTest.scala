package com.twitter.finatra.http.request

import com.twitter.finagle.http.{Message, Method}
import com.twitter.finagle.http.Method._
import com.twitter.inject.Test

class RequestBuilderTest extends Test {

  test("get") {
    val request = RequestBuilder
      .get("/abc")
      .header("c", "3")
      .headers("a" -> "1", "b" -> "2")

    request.uri should be("/abc")
    request.method should be(Get)

    request.headerMap should be(Map("a" -> "1", "b" -> "2", "c" -> "3"))
  }

  test("post") {
    val request = RequestBuilder
      .post("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Post, request)
  }

  test("put") {
    val request = RequestBuilder
      .put("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Put, request)
  }

  test("put with form params") {
    val request = RequestBuilder
      .put("/abc")
      .body("Pcode=9999&Locality=A%20New%20Location", Message.ContentTypeWwwForm)
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    request.uri should be("/abc")
    request.method should be(Put)

    request.headerMap should be(
      Map(
        "a" -> "1",
        "b" -> "2",
        "c" -> "3",
        "Content-Length" -> "38",
        "Content-Type" -> Message.ContentTypeWwwForm
      )
    )

    request.contentString should be("Pcode=9999&Locality=A%20New%20Location")
  }

  test("patch") {
    val request = RequestBuilder
      .patch("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Patch, request)
  }

  test("delete") {
    val request = RequestBuilder
      .delete("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Delete, request)
  }

  test("head") {
    val request = RequestBuilder
      .head("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Head, request)
  }

  test("trace") {
    val request = RequestBuilder
      .trace("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Trace, request)
  }

  test("connect") {
    val request = RequestBuilder
      .connect("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Connect, request)
  }

  test("options") {
    val request = RequestBuilder
      .options("/abc")
      .body("testbody", "foo/bar")
      .headers(Seq("c" -> "3"))
      .headers("a" -> "1", "b" -> "2")

    assertRequestWithBody(Options, request)
  }

  test("post json") {
    val request = RequestBuilder
      .post("/abc")
      .body("{}")

    request.headerMap("Content-Type") should be("application/json;charset=utf-8")
    request.contentString should be("{}")
  }

  test("post json with resource") {
    val request = RequestBuilder
      .post("/abc")
      .bodyFromResource("/test_resource.json")

    request.headerMap("Content-Type") should be("application/json;charset=utf-8")
    request.contentString should be("""{"a": "1", "b": "2"}""")
  }

  test("post utf8 content") {
    val request = RequestBuilder
      .post("/abc")
      .body("ＴＥＳＴＢＯＤＹ")

    request.headerMap("Content-Length") should be("24")
  }

  def assertRequestWithBody(expectedMethod: Method, request: RequestBuilder): Unit = {
    request.uri should be("/abc")
    request.method should be(expectedMethod)

    request.headerMap should be(
      Map("a" -> "1", "b" -> "2", "c" -> "3", "Content-Length" -> "8", "Content-Type" -> "foo/bar")
    )

    request.contentString should be("testbody")
  }
}
