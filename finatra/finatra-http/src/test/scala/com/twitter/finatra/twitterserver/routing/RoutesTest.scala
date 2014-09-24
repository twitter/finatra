package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.http.{Response, Request => FinagleRequest}
import com.twitter.finatra.Request
import com.twitter.finatra.test.Test
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._

class RoutesTest extends Test {

  "constant route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/")), GET)

    routes.handle(
      FinagleRequest("/groups/")).isDefined should be(true)

    routes.handle(
      FinagleRequest("/groups")).isDefined should be(false)

    routes.handle(
      FinagleRequest("/foo")).isDefined should be(false)
  }

  "constant route with optional trailing slash" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/?")), GET)

    routes.handle(
      FinagleRequest("/groups/")).isDefined should be(true)

    routes.handle(
      FinagleRequest("/groups")).isDefined should be(true)
  }

  "path pattern route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/:id")), GET)

    routes.handle(
      FinagleRequest("/groups/1")).isDefined should be(true)

    routes.handle(
      FinagleRequest("/groups/")).isDefined should be(false)
  }

  "path pattern route with optional trailing slash" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/:id/foo/?")), GET)

    routes.handle(
      FinagleRequest("/groups/1/foo/")).isDefined should be(true)

    routes.handle(
      FinagleRequest("/groups/1/foo")).isDefined should be(true)
  }

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  def createRoute(method: HttpMethod, path: String): Route = {
    Route(
      method = method,
      path = path,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response])
  }
}
