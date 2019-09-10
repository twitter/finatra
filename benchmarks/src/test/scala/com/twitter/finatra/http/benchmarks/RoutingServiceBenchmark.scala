package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.http.internal.routing.{Route, RoutingService}
import com.twitter.util.Future
import org.openjdk.jmh.annotations._
import scala.reflect.classTag

/**
 * ./sbt 'project benchmarks' 'jmh:run RoutingServiceBenchmark'
 */
@State(Scope.Thread)
class RoutingServiceBenchmark extends StdBenchAnnotations with HttpBenchmark {

  val routes: Seq[Route] = Seq(
    route("/groups/verified/vip/top10/"),
    route("/groups/verified/vip/top10/:id"),
    route("/tasks/easy/top10/:id"),
    route("/tasks/starter/top10/:id"),
    route("/users/test/temp/top10/"),
    route("/users/test/temp/top10/:id"),
    route("/groups/vip/top10/"),
    route("/groups/vip/top10/:id"),
    route("/groups/vip/top10/:id/:name"),
    route("/foo/bar/baz/top10/"),
    route("/foo/bar/baz/:id"),
    route("/foo/baz/baz/:name"),
    route("/groups/verified/vip/top5/"),
    route("/groups/verified/vip/top5/:id"),
    route("/tasks/easy/top5/:id"),
    route("/tasks/starter/top5/:id"),
    route("/users/test/temp/top5/"),
    route("/users/test/temp/top5/:id"),
    route("/groups/vip/top5/"),
    route("/groups/vip/top5/:id"),
    route("/groups/vip/top5/:id/:name"),
    route("/foo/bar/baz/top5/"),
    route("/foo/bar/baz/top5/:id"),
    route("/foo/baz/baz/top5/:name")
  )

  val routingService: RoutingService = new RoutingService(routes)
  val getRequest1: Request = Request("/foo/bar/baz/top5/")
  val getRequest2: Request = Request("/groups/vip/top5/123/jack")

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
}
