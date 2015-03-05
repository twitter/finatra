package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.internal.routing.{Route, RoutingService}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class RoutingServiceBenchmark {

  def defaultCallback(request: Request) = {
    Future.value(Response())
  }

  val route = Route(
    method = HttpMethod.GET,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val routingService = new RoutingService(
    routes = Seq(route))

  val getRequest = Request("/groups/")

  @Benchmark
  def testRoutingController1() = {
    routingService.apply(getRequest)
  }
}