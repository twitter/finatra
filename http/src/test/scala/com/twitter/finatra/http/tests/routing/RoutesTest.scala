package com.twitter.finatra.http.tests.routing

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.routing.{Route, Routes}
import com.twitter.inject.Test
import com.twitter.util.Future
import org.scalatest.OptionValues

class RoutesTest extends Test with OptionValues {

  "constant route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    routes.handle(
      Request("/groups/"), bypassFilters = false) should be('defined)

    routes.handle(
      Request("/groups"), bypassFilters = false) should be('empty)

    routes.handle(
      Request("/foo"), bypassFilters = false) should be('empty)
  }

  "path pattern route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/:id")), Method.Get)

    routes.handle(
      Request("/groups/1"), bypassFilters = false) should be('defined)

    routes.handle(
      Request("/groups/"), bypassFilters = false) should be('empty)
  }

  "route info" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    val request = Request("/groups/")
    routes.handle(request, bypassFilters = false) should be('defined)

    RouteInfo(request).value should be(RouteInfo("my_endpoint", "/groups/"))
  }

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  def createRoute(method: Method, path: String): Route = {
    Route(
      name = "my_endpoint",
      method = method,
      path = path,
      admin = false,
      index = None,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response],
      routeFilter = Filter.identity,
      filter = Filter.identity)
  }
}
