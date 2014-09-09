package com.twitter.finatra.test

import com.twitter.finagle.http.{Method, Response, Request => FinagleRequest}
import com.twitter.finatra.request.RouteParams
import com.twitter.finatra.twitterserver.routing.RoutingController
import com.twitter.finatra.{Controller, Request}
import com.twitter.util.{Await, Future}

//POC for unit testing finatra controllers
//TODO: Add support for other HTTP methods
abstract class ControllerTest extends HttpTest {

  val controller: Controller

  lazy val routingController =
    new RoutingController(controller.routes)

  protected def performGet(uri: String): Future[Response] = {
    routingController(getRequest(uri))
  }

  protected def getAndWait[T: Manifest](uri: String): T = {
    val response = Await.result(performGet(uri))
    println(response + " " + response.contentString)
    val str = response.contentString
    mapper.parse[T](str)
  }

  protected def getRequest(uri: String) = {
    new Request(
      FinagleRequest(Method.Get, uri),
      routeParams = RouteParams.empty)
  }
}
