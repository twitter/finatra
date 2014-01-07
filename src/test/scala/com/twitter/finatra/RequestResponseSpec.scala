package com.twitter.finatra

import com.twitter.finatra.test.FlatSpecHelper
import com.twitter.finagle.http.Cookie
import org.jboss.netty.handler.codec.http.DefaultCookie

class RequestResponseSpec extends FlatSpecHelper {

  class ExampleApp extends Controller {
    get("/") { request =>
      request.response.setStatusCode(429)
      render.plain("hello world").toFuture
    }

    get("/cookies") { request =>
      request.response.cookies.add(new Cookie(new DefaultCookie("foo", "bar")))
      render.plain("asd").cookie("foo2", "bar2").toFuture
    }

    get("/headers") { request =>
      request.response.headers.add("foo", "bar")
      render.plain("foo").toFuture
    }

    get("/content") { request =>
      request.response.setContentString("foo")
      render.ok.toFuture
    }
  }

  val server = new FinatraServer
  server.register(new ExampleApp)

  "Response" should "be tied to the request" in {
    get("/")
    response.code should equal (429)
  }

  "Response" should "include original cookies, and rendered cookies" in {
    get("/cookies")
    response.originalResponse.cookies.contains("foo") should equal(true)
    response.originalResponse.cookies.contains("foo2") should equal(true)
  }

  "Response" should "carry along headers" in {
    get("/headers")
    response.originalResponse.headerMap.contains("foo") should equal(true)
  }

  "Response" should "carry along content" in {
    get("/content")
    response.originalResponse.getContentString() should equal("foo")
  }

}
