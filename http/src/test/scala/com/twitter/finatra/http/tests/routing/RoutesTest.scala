package com.twitter.finatra.http.tests.routing

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.routing.{Route, Routes}
import com.twitter.inject.Test
import com.twitter.util.Future
import org.scalatest.OptionValues

class RoutesTest extends Test with OptionValues {

  test("constant route") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    routes.handle(Request("/groups/")) should be('defined)

    routes.handle(Request("/groups")) should be('empty)

    routes.handle(Request("/foo")) should be('empty)
  }

  test("constant route (bypassFilters = true)") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    routes.handle(Request("/groups/"), bypassFilters = true) should be('defined)

    routes.handle(Request("/groups"), bypassFilters = true) should be('empty)

    routes.handle(Request("/foo"), bypassFilters = true) should be('empty)
  }

  test("constant route with optional trailing slashes") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/?")), Method.Get)

    routes.handle(Request("/groups/")) should be('defined)

    routes.handle(Request("/groups")) should be('defined)

    routes.handle(Request("/foo")) should be('empty)
  }

  test("constant route with optional trailing slashes (bypassFilters = true)") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/?")), Method.Get)

    routes.handle(Request("/groups/"), bypassFilters = true) should be('defined)

    routes.handle(Request("/groups"), bypassFilters = true) should be('defined)

    routes.handle(Request("/foo"), bypassFilters = true) should be('empty)
  }

  test("constant route with wildcard") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:*")), Method.Get)

    routes.handle(Request("/groups/index.html")) should be('defined)

    routes.handle(Request("/groups/index.html/")) should be('defined)

    routes.handle(Request("/foo")) should be('empty)
  }

  test("constant route with wildcard and optional trailing slashes") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:*/?")), Method.Get)

    routes.handle(Request("/groups/index.html")) should be('defined)

    routes.handle(Request("/groups/index.html/")) should be('defined)

    routes.handle(Request("/foo")) should be('empty)
  }

  test("path pattern route") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:id")), Method.Get)

    routes.handle(Request("/groups/1")) should be('defined)

    routes.handle(Request("/groups/")) should be('empty)
  }

  test("path pattern route (bypassFilters = true)") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:id")), Method.Get)

    routes.handle(Request("/groups/1"), bypassFilters = true) should be('defined)

    routes.handle(Request("/groups/"), bypassFilters = true) should be('empty)
  }

  test("path pattern route with optional trailing slash") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:id/?")), Method.Get)

    routes.handle(Request("/groups/1")) should be('defined)

    routes.handle(Request("/groups/1/")) should be('defined)

    routes.handle(Request("/groups/")) should be('empty)
  }

  test("path pattern route with wildcard") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:id/:*")), Method.Get)

    routes.handle(Request("/groups/1/foo")) should be('defined)

    routes.handle(Request("/groups/1/foo/")) should be('defined)

    routes.handle(Request("/groups/")) should be('empty)
  }

  test("path pattern route with wildcard and optional trailing slash identifier") {
    val routes =
      Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/:id/:*/?")), Method.Get)

    routes.handle(Request("/groups/1/foo")) should be('defined)

    routes.handle(Request("/groups/1/foo/")) should be('defined)

    routes.handle(Request("/groups/")) should be('empty)
  }

  test("route info") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    val request = Request("/groups/")
    routes.handle(request) should be('defined)

    RouteInfo(request).value should be(RouteInfo("my_endpoint", "/groups/"))
  }

  test("route info (bypassFilters = true)") {
    val routes = Routes.createForMethod(Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    val request = Request("/groups/")
    routes.handle(request, bypassFilters = true) should be('defined)

    RouteInfo(request).value should be(RouteInfo("my_endpoint", "/groups/"))
  }

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  def createRoute(method: Method, path: String): Route = {
    Route(
      name = "my_endpoint",
      method = method,
      uri = path,
      admin = false,
      index = None,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response],
      routeFilter = Filter.identity,
      filter = Filter.identity
    )
  }
}
