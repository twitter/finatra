package com.twitter.finatra.http.tests.routing

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.routing.{Route, Routes}
import com.twitter.finatra.http.routing.Prefix
import com.twitter.inject.Test
import com.twitter.util.Future
import org.scalatest.OptionValues

class RoutesTest extends Test with OptionValues {

  "constant route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    routes.handle(
      Request("/groups/")) should be('defined)

    routes.handle(
      Request("/groups")) should be('empty)

    routes.handle(
      Request("/foo")) should be('empty)
  }

  "path pattern route" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/:id")), Method.Get)

    routes.handle(
      Request("/groups/1")) should be('defined)

    routes.handle(
      Request("/groups/")) should be('empty)
  }

  "route info" in {
    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/")), Method.Get)

    val request = Request("/groups/")
    routes.handle(request) should be('defined)

    RouteInfo(request).value should be(RouteInfo("my_endpoint", "/groups/"))
  }

  "prefixed route" in {
    val prefix = new Prefix { def prefix: String = "/prefix" }

    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/", prefix)), Method.Get)

    routes.handle(
      Request("/prefix/groups/")) should be('defined)

    routes.handle(
      Request("/groups/")) should be('empty)
  }

  "doubly prefixed route" in {
    val prefix = new Prefix { def prefix: String = "/prefix" }
    val other = new Prefix { def prefix: String = "/other" }

    val routes = Routes.createForMethod(
      Seq(createRoute(Method.Get, "/groups/", prefix andThen other)), Method.Get)

    routes.handle(
      Request("/prefix/other/groups/")) should be('defined)

    routes.handle(
      Request("/groups/")) should be('empty)

    routes.handle(
      Request("/prefix/groups/")) should be('empty)

    routes.handle(
      Request("/other/groups/")) should be('empty)
  }

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  def createRoute(method: Method, path: String, prefix: Prefix = Prefix.empty): Route = {
    Route(
      name = "my_endpoint",
      method = method,
      prefix = prefix,
      path = path,
      admin = false,
      adminIndexInfo = None,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response],
      filter = Filter.identity)
  }
}
