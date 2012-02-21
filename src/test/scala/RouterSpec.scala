package com.posterous.finatra 

import org.junit.Test
import com.codahale.simplespec.Spec
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpMethod, HttpVersion}
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._


object FakeApp extends FinatraApp {
  get("/") { "resp" }

  get("/other") { "otherresp" }
  head("/other") { "specialresp" }
}
 
class RouterSpec extends Spec {

  class `GET '/'` {

    FakeApp

    var request = Request(HttpMethod.GET, "/")
    var response = Router.dispatch(request)

    @Test def `returns 200` = {
      response.statusCode.must(be(200))
    }
    
    @Test def `returns a response` = {
      response.must(beA[Response])
    }

    @Test def `responds with 'resp'` = {
      response.content.toString("UTF8").must(be("resp"))
    }

  }

  class `HEAD '/' (curl -I)` {

    FakeApp

    var request = Request(HttpMethod.HEAD, "/")
    var response = Router.dispatch(request)

    @Test def `returns 200` = {
      response.statusCode.must(be(200))
    }
    
    @Test def `returns a response` = {
      response.must(beA[Response])
    }

    @Test def `responds with 'resp'` = {
      response.content.toString("UTF8").must(be("resp"))
    }

  }

  class `HEAD '/other' (curl -I)` {

    FakeApp

    var request = Request(HttpMethod.HEAD, "/other")
    var response = Router.dispatch(request)

    @Test def `returns 200` = {
      response.statusCode.must(be(200))
    }
    
    @Test def `returns a response` = {
      response.must(beA[Response])
    }

    @Test def `responds with 'resp'` = {
      response.content.toString("UTF8").must(be("specialresp"))
    }

  }

  class `GET '/other' (curl -I)` {

    FakeApp

    var request = Request(HttpMethod.GET, "/other")
    var response = Router.dispatch(request)

    @Test def `returns 200` = {
      response.statusCode.must(be(200))
    }
    
    @Test def `returns a response` = {
      response.must(beA[Response])
    }

    @Test def `responds with 'resp'` = {
      response.content.toString("UTF8").must(be("otherresp"))
    }

  }
}

