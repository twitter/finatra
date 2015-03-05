package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.benchmarks.alternatives.Route2NoPrivateThis
import com.twitter.finatra.internal.routing.Route
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, TimeValue}

@State(Scope.Thread)
class RouteBenchmark {

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  val route = Route(
    method = HttpMethod.POST,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val route2 = Route2NoPrivateThis(
    method = HttpMethod.POST,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq())

  val routeWithPathParams = Route(
    method = HttpMethod.POST,
    path = "/groups/:id",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val route2WithPathParams = Route2NoPrivateThis(
    method = HttpMethod.POST,
    path = "/groups/:id",
    callback = defaultCallback,
    annotations = Seq())

  val postGroupsPath = "/groups/"
  val postGroupsRequest = Request(HttpMethod.POST, postGroupsPath)

  val postGroups123Path = postGroupsPath + "123"
  val postGroups123Request = Request(HttpMethod.POST, postGroups123Path)

  @Benchmark
  def testRoute() = {
    route.handle(postGroupsRequest, postGroupsPath)
  }

  @Benchmark
  def testRoute2NoPrivateThis() = {
    route2.handle(postGroupsRequest, postGroupsPath)
  }

  @Benchmark
  def testRouteWithPathParams() = {
    routeWithPathParams.handle(postGroups123Request, postGroups123Path)
  }

  @Benchmark
  def testRoute2NoPrivateThisWithPathParams() = {
    route2WithPathParams.handle(postGroups123Request, postGroups123Path)
  }
}


object RouteBenchmarkMain {
  def main(args: Array[String]) {
    new Runner(new OptionsBuilder()
      .include(".*RouteBenchmark.*")
      .warmupIterations(5)
      .warmupTime(TimeValue.seconds(5))
      .measurementIterations(5)
      .measurementTime(TimeValue.seconds(5))
      .forks(1)
      //.addProfiler(classOf[StackProfiler])
      //.jvmArgsAppend("-Djmh.stack.period=1")
      .build()).run()
  }
}
