package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.internal.routing.{Route, RoutingService}
import com.twitter.inject.requestscope.{FinagleRequestScope, FinagleRequestScopeFilter}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class FinagleRequestScopeBenchmark {

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

  val routingContoller = new RoutingService(routes = Seq(route))

  val getRequest = Request("/groups/")

  val finagleRequestScope = new FinagleRequestScope()

  val finagleRequestScopeFilter =
    new FinagleRequestScopeFilter[Request, Response](finagleRequestScope)

  val filtersAndService =
    finagleRequestScopeFilter andThen
      routingContoller

  @Benchmark
  def timeServiceWithRequestScopeFilter() = {
    filtersAndService.apply(getRequest)
  }
}
