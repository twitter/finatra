package com.posterous.finatra 

import org.junit.Test
import com.codahale.simplespec.Spec
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpMethod, HttpVersion}
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._


object FakeApp extends FinatraApp {
  get("/") { "resp" }
}
 
class RouterSpec extends Spec {

  class `route hashing` {

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


}

