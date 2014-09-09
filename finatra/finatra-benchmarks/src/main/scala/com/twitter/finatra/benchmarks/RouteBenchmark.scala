package com.twitter.finatra.benchmarks

import com.twitter.finagle.http.{Response, Request => FinagleRequest}
import com.twitter.finatra.Request
import com.twitter.finatra.marshalling.MessageBodyManager
import com.twitter.finatra.twitterserver.routing.Route
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class RouteBenchmark {

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  val messageBodyManager = new MessageBodyManager(null, null, null)

  val route = Route(
    method = HttpMethod.POST,
    path = "/groups/",
    callback = defaultCallback,
    annotations = Seq(),
    requestClass = classOf[Request],
    responseClass = classOf[Response])

  val getRequest = FinagleRequest("/groups/")

  @Benchmark
  def testRouteWithoutPathParams() = {
    route.handle(getRequest)
  }
}