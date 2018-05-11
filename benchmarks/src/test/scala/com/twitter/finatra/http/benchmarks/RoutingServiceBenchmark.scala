package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.http.internal.routing.{Route, RoutingService}
import com.twitter.util.Future
import org.openjdk.jmh.annotations._

/**
 * ./sbt 'project benchmarks' 'jmh:run RoutingServiceBenchmark'
 */
@State(Scope.Thread)
class RoutingServiceBenchmark
  extends StdBenchAnnotations
  with HttpBenchmark {

  val routes: Seq[Route] = Seq(
    route("/groups/"),
    route("/groups/:id"),
    route("/tasks/"),
    route("/tasks/:id"),
    route("/foo/"),
    route("/foo/:id"),
    route("/users/"),
    route("/users/:id")
  )

  val routingService: RoutingService = new RoutingService(routes)
  val getRequest1: Request = Request("/users/")
  val getRequest2: Request = Request("/users/123")

  @Benchmark
  def timeOldLastConstant(): Future[Response] = {
    routingService.apply(getRequest1)
  }

  @Benchmark
  def timeOldLastNonConstant(): Future[Response] = {
    routingService.apply(getRequest2)
  }

  def route(path: String) =
    Route(
      name = path,
      method = Method.Get,
      uri = path,
      admin = false,
      index = None,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response],
      routeFilter = Filter.identity,
      filter = Filter.identity
    )
}
