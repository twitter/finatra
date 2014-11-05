package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.***REMOVED***.authentication.filters.ClientApplicationFilter
import com.twitter.finatra.requestscope.{FinagleRequestScope, FinagleRequestScopeFilter}
import com.twitter.finatra.routing.{Route, RoutingService}
import com.twitter.finatra.{Request => FinatraRequest}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, TimeValue}

@State(Scope.Thread)
class FinagleRequestScopeBenchmark {

  def defaultCallback(request: FinatraRequest) = {
    Future.value(Response())
  }

  val route = Route(
    method = HttpMethod.GET,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[FinatraRequest],
    responseClass = classOf[Response])

  val routingContoller = new RoutingService(routes = Seq(route))

  val getRequest = Request("/groups/")

  val finagleRequestScope = new FinagleRequestScope()

  val finagleRequestScopeFilter =
    new FinagleRequestScopeFilter(finagleRequestScope)

  val clientApplicationAuthFilter =
    new ClientApplicationFilter(finagleRequestScope)

  val filtersAndService =
    finagleRequestScopeFilter andThen
      clientApplicationAuthFilter andThen
      routingContoller

  @Benchmark
  def testRequestScope() = {
    filtersAndService.apply(getRequest)
  }
}

object FinagleRequestScopeBenchmarkMain {
  def main(args: Array[String]) {
    new Runner(new OptionsBuilder()
      .include(".*FinagleRequestScopeBenchmark.*")
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
