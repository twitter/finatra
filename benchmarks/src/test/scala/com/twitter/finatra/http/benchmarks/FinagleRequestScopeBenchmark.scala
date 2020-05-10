package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.http.internal.routing.{Route, RoutingService}
import com.twitter.inject.requestscope.{FinagleRequestScope, FinagleRequestScopeFilter}
import com.twitter.util.Future
import org.openjdk.jmh.annotations._
import scala.reflect.classTag

/**
 * ./sbt 'project benchmarks' 'jmh:run FinagleRequestScopeBenchmark'
 */
@State(Scope.Thread)
class FinagleRequestScopeBenchmark extends StdBenchAnnotations with HttpBenchmark {

  val route = Route(
    name = "groups",
    method = Method.Get,
    uri = "/groups/",
    clazz = this.getClass,
    admin = false,
    index = None,
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classTag[Request],
    responseClass = classTag[Response],
    routeFilter = Filter.identity,
    filter = Filter.identity
  )

  val routingController: RoutingService = new RoutingService(routes = Seq(route))

  val getRequest: Request = Request("/groups/")

  val finagleRequestScope: FinagleRequestScope = new FinagleRequestScope()

  val finagleRequestScopeFilter: FinagleRequestScopeFilter[Request, Response] =
    new FinagleRequestScopeFilter[Request, Response](finagleRequestScope)

  val filtersAndService: Service[Request, Response] =
    finagleRequestScopeFilter.andThen(routingController)

  @Benchmark
  def timeServiceWithRequestScopeFilter(): Future[Response] = {
    filtersAndService.apply(getRequest)
  }
}
