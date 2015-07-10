package com.twitter.finatra.http.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.routing.{Route, Routes}
import com.twitter.inject.Test
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.scalatest.OptionValues

class RoutesTest extends Test with OptionValues {

  "constant route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/")), GET)

    routes.handle(
      Request("/groups/")) should be('defined)

    routes.handle(
      Request("/groups")) should be('empty)

    routes.handle(
      Request("/foo")) should be('empty)
  }

  "constant route with optional trailing slash" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/?")), GET)

    routes.handle(
      Request("/groups/")) should be('defined)

    routes.handle(
      Request("/groups")) should be('defined)
  }

  "path pattern route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/:id")), GET)

    routes.handle(
      Request("/groups/1")) should be('defined)

    routes.handle(
      Request("/groups/")) should be('empty)
  }

  "path pattern route with optional trailing slash" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/:id/foo/?")), GET)

    routes.handle(
      Request("/groups/1/foo/")) should be('defined)

    routes.handle(
      Request("/groups/1/foo")) should be('defined)
  }

  "route info" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(GET, "/groups/")), GET)

    val request = Request("/groups/")
    routes.handle(request) should be('defined)

    RouteInfo(request).value should be(RouteInfo("groups", GET))
  }

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  def createRoute(method: HttpMethod, path: String): Route = {
    Route(
      name = "groups",
      method = method,
      path = path,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response])
  }
}
