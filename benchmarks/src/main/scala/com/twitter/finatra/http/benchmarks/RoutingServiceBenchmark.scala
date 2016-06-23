package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.internal.routing.{Route, RoutingService}
import com.twitter.util.Future
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class RoutingServiceBenchmark {

  def defaultCallback(request: Request) = {
    Future.value(Response())
  }

  val routes = Seq(
    route("/groups/"),
    route("/groups/:id"),
    route("/tasks/"),
    route("/tasks/:id"),
    route("/foo/"),
    route("/foo/:id"),
    route("/users/"),
    route("/users/:id"))

  val routingService = new RoutingService(routes)

  val getRequest1 = Request("/users/")
  val getRequest2 = Request("/users/123")

  @Benchmark
  def timeOldLastConstant() = {
    routingService.apply(getRequest1)
  }

  @Benchmark
  def timeOldLastNonConstant() = {
    routingService.apply(getRequest2)
  }

  def route(path: String) = Route(
    name = path,
    method = Method.Get,
    path = path,
    admin = false,
    adminIndexInfo = None,
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response],
    filter = Filter.identity)
}
