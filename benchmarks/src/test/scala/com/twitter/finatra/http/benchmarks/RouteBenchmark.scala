package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.util.Future
import org.openjdk.jmh.annotations._
import scala.reflect.classTag

/**
 * ./sbt 'project benchmarks' 'jmh:run RouteBenchmark'
 */
@State(Scope.Thread)
class RouteBenchmark extends StdBenchAnnotations with HttpBenchmark {

  val route = Route(
    name = "groups",
    method = Method.Post,
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

  val routeWithPathParams = Route(
    name = "groups",
    method = Method.Post,
    uri = "/groups/:id",
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

  val postGroupsPath: String = "/groups/"
  val postGroupsRequest: Request = Request(Method.Post, postGroupsPath)

  val postGroups123Path: String = postGroupsPath + "123"
  val postGroups123Request: Request = Request(Method.Post, postGroups123Path)

  val postGroupsUrlEncodedPath: String = postGroupsPath + "%24%23%26" // $#&
  val postGroupsUrlEncodedRequest: Request = Request(Method.Post, postGroupsUrlEncodedPath)

  @Benchmark
  def testRoute(): Option[Future[Response]] = {
    route.handle(request = postGroupsRequest, routeParams = Map.empty, bypassFilters = false)
  }

  @Benchmark
  def testRouteWithPathParams(): Option[Future[Response]] = {
    routeWithPathParams.handle(
      request = postGroups123Request,
      routeParams = Map("id" -> "123"),
      bypassFilters = false)
  }

  @Benchmark
  def testRouteWithUrlEncodedPathParams(): Option[Future[Response]] = {
    routeWithPathParams.handle(
      request = postGroupsUrlEncodedRequest,
      routeParams = Map.empty,
      bypassFilters = false)
  }
}
