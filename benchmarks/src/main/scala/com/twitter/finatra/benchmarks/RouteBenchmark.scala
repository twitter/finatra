package com.twitter.finatra.benchmarks

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.util.Future
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class RouteBenchmark {

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  val route = Route(
    name = "groups",
    method = Method.Post,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response],
    filter = Filter.identity)

  val routeWithPathParams = Route(
    name = "groups",
    method = Method.Post,
    path = "/groups/:id",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response],
    filter = Filter.identity)

  val postGroupsPath = "/groups/"
  val postGroupsRequest = Request(Method.Post, postGroupsPath)

  val postGroups123Path = postGroupsPath + "123"
  val postGroups123Request = Request(Method.Post, postGroups123Path)

  @Benchmark
  def testRoute() = {
    route.handle(postGroupsRequest, postGroupsPath)
  }

  @Benchmark
  def testRouteWithPathParams() = {
    routeWithPathParams.handle(postGroups123Request, postGroups123Path)
  }
}
