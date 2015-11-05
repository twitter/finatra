package com.twitter.finatra.multiserver.Add2HttpServer

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.util.Future
import javax.inject.Inject

class Add2Controller @Inject()(
  add1Client: HttpClient,
  responseBuilder: ResponseBuilder)
  extends Controller {

  get("/add2") { request: Request =>
    for {
      numPlus1 <- add1(request.getIntParam("num"))
      numPlus2 <- add1(numPlus1)
    } yield numPlus2
  }

  private def add1(num: Int): Future[Int] = {
    val add1Request = RequestBuilder.get(s"/add1?num=$num")
    add1Client.execute(add1Request) map { add1Response =>
      if (add1Response.status == Status.Ok)
        add1Response.getContentString().toInt
      else
        throw responseBuilder.serviceUnavailable.toException
    }
  }
}