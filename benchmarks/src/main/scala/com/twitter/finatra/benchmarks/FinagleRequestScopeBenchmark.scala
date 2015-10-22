package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.internal.routing.{Route, RoutingService}
import com.twitter.inject.requestscope.{FinagleRequestScope, FinagleRequestScopeFilter}
import com.twitter.util.Future
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class FinagleRequestScopeBenchmark {

  def defaultCallback(request: Request) = {
    Future.value(Response())
  }

  val route = Route(
    name = "groups",
    method = Method.Get,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val routingController = new RoutingService(routes = Seq(route))

  val getRequest = Request("/groups/")

  val finagleRequestScope = new FinagleRequestScope()

  val finagleRequestScopeFilter =
    new FinagleRequestScopeFilter[Request, Response](finagleRequestScope)

  val filtersAndService =
    finagleRequestScopeFilter andThen
      routingController

  @Benchmark
  def timeServiceWithRequestScopeFilter() = {
    filtersAndService.apply(getRequest)
  }
}
