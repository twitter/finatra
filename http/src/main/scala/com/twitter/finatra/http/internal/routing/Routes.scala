package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.UnsupportedMethodException
import com.twitter.inject.conversions.iterable._
import com.twitter.util.Future

private[http] object Routes {
  def createRoutes(routes: Seq[Route]): Routes = new Routes(routes.toArray)
}

// optimized
private[http] class Routes(routes: Array[Route]) {
  // assert unique paths per method
  routes.groupBy(_.method).foreach {
    case (_, routesPerMethod) =>
      val distinctRoutes = routesPerMethod.toSeq.distinctBy(_.path)
      assert(
        routesPerMethod.length == distinctRoutes.length,
        "Found non-unique routes " + routesPerMethod
          .diff(distinctRoutes).map(_.summary).mkString(", ")
      )
  }

  private[this] val (constantRoutes, nonConstantRoutes) = routes.partition(_.constantRoute)
  private[this] val constantRouteMap: ConstantRouteMap = new ConstantRouteMap(constantRoutes)
  private[this] val trie: Trie = new Trie(nonConstantRoutes)

  def handle(request: Request, bypassFilters: Boolean = false): Option[Future[Response]] = {
    // store path since Request#path is derived
    val path = request.path
    val method = request.method

    val constantRoute = constantRouteMap.find(path, method)
    constantRoute.routeOpt match {
      case Some(matchedConstantRoute) =>
        // found a constant route match
        matchedConstantRoute.handleMatch(request, bypassFilters)
      case None =>
        // otherwise walk the trie
        trie.find(path, method) match {
          case Some(RouteAndParameter(nonConstantRoute, routeParams)) =>
            nonConstantRoute.handle(request, bypassFilters, routeParams)
          case None if constantRoute.methodNotAllowed =>
            throw new UnsupportedMethodException(
              "The method " + method + " is not allowed on path " + path)
          case None => None
        }
    }
  }
}
