package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.http.Method
import com.twitter.finatra.http.AnyMethod
import scala.collection.mutable.{AnyRefMap => AMap}

private[http] case class MatchedConstantRoute (routeOpt: Option[Route] = None, methodNotAllowed: Boolean = false)

private[http] class ConstantRouteMap(constantRoutes: Seq[Route]) {
  // Use AnyRefMap for faster look up performance
  // Map route path to another map of method name to route
  private[this] val map: AMap[String, AMap[String, Route]] = AMap.empty

  for (route <- constantRoutes) {
    val path = route.path
    storeRoute(path, route)
    // When the route has an optional trailing slash identifier, store both paths with or without trailing slash
    if (route.hasOptionalTrailingSlash) {
      val pathWithoutSlash = path.substring(0, path.length - 1)
      storeRoute(pathWithoutSlash, route)
    }
  }

  private[http] def find(path: String, method: Method): MatchedConstantRoute = {
    val methodName = method.name
    map.get(path) match {
      case Some(routes) =>
        routes.get(methodName).orElse(routes.get(AnyMethod.name)) match {
          case None => MatchedConstantRoute(methodNotAllowed = true)
          case routeOpt => MatchedConstantRoute(routeOpt)
        }
      case None =>
        MatchedConstantRoute()
    }
  }

  /* ----------------------------------------- *
   *               Util Methods
   * ----------------------------------------- */

  private[this] def storeRoute(path: String, route: Route): Unit = {
    map.get(path) match {
      case Some(routes) =>
        routes.put(route.method.name, route)
      case None =>
        map.put(path, AMap(route.method.name -> route))
    }
  }
}
