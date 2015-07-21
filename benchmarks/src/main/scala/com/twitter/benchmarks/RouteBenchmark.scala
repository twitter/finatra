package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class RouteBenchmark {

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  val route = Route(
    name = "groups",
    method = HttpMethod.POST,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val routeWithPathParams = Route(
    name = "groups",
    method = HttpMethod.POST,
    path = "/groups/:id",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val postGroupsPath = "/groups/"
  val postGroupsRequest = Request(HttpMethod.POST, postGroupsPath)

  val postGroups123Path = postGroupsPath + "123"
  val postGroups123Request = Request(HttpMethod.POST, postGroups123Path)

  @Benchmark
  def testRoute() = {
    route.handle(postGroupsRequest, postGroupsPath)
  }

  @Benchmark
  def testRouteWithPathParams() = {
    routeWithPathParams.handle(postGroups123Request, postGroups123Path)
  }
}
