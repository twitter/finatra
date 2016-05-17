package com.twitter.finatra.http.tests.integration.multiserver.add2server

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import javax.inject.Inject

class Add2Controller @Inject()(
  add1Client: HttpClient)
  extends Controller {

  get("/add2") { request: Request =>
    for {
      numPlus1 <- add1(request.getIntParam("num"))
      numPlus2 <- add1(numPlus1)
    } yield numPlus2
  }

  private def add1(num: Int) = {
    val request = RequestBuilder.get(s"/add1?num=$num")
    add1Client.execute(request) map { response =>
      response.getContentString().toInt
    }
  }
}