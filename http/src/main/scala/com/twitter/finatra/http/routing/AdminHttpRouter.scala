package com.twitter.finatra.http.routing

import com.twitter.finagle.http.{HttpMuxer, Method}
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.finatra.http.request.AnyMethod
import com.twitter.inject.Logging
import com.twitter.server.AdminHttpServer
import com.twitter.server.filters.AdminThreadPoolFilter
import com.twitter.util.lint.{Category, GlobalRules, Issue, Rule}

private[http] object AdminHttpRouter extends Logging {

  /**
   * Adds routes to the TwitterServer HTTP Admin Interface.
   *
   * Constant routes which do not begin with /admin/finatra can be added to the admin index,
   * all other routes cannot. Only constant /GET or /POST routes will be eligible to be added
   * to the admin index.
   *
   * NOTE: beforeRouting = true filters will not be properly evaluated on adminIndexRoutes
   * since the local Muxer in the AdminHttpServer does exact route matching before marshalling
   * to the handler service (where the filter is composed). Thus if this filter defines a route
   * it will not be routed to by the local Muxer. Any beforeRouting = true filters should act
   * only on paths behind /admin/finatra.
   */
  def addAdminRoutes(
    server: AdminHttpServer,
    router: HttpRouter,
    twitterServerAdminRoutes: Seq[AdminHttpServer.Route]
  ): Unit = {
    val allTwitterServerAdminRoutes = twitterServerAdminRoutes.map(_.path).union(HttpMuxer.patterns)
    val duplicates = allTwitterServerAdminRoutes.intersect(router.routesByType.admin.map(_.path))
    if (duplicates.nonEmpty) {
      val errorMsg = "Duplicating pre-defined TwitterServer AdminHttpServer routes is not allowed."
      val message = "The following routes are duplicates of pre-defined TwitterServer admin routes:"
      error(s"$message \n\t${duplicates.mkString("\n\t")}")
      error(errorMsg)
      throw new java.lang.AssertionError(errorMsg)
    }

    // Partition routes into admin index routes and admin rich handler routes
    val (adminIndexRoutes, adminRichHandlerRoutes) = router.routesByType.admin.partition { route =>
      // admin index routes cannot start with /admin/finatra/ and must be a constant route
      !route.path.startsWith(HttpRouter.FinatraAdminPrefix) && route.constantRoute
    }

    // Run linting rule for routes
    GlobalRules.get.add(
      Rule(
        Category.Configuration,
        "Non-indexable HTTP Admin Interface Finatra Routes",
        s"""Only constant /GET or /POST routes prefixed with "/admin" that DO NOT begin
           |with "${HttpRouter.FinatraAdminPrefix}" can be added to the TwitterServer
           |HTTP Admin Interface index.""".stripMargin
      ) {
        Seq(
          checkRoutesWithRouteIndex(adminRichHandlerRoutes) { _ =>
            true
          },
          checkRoutesWithRouteIndex(adminIndexRoutes) { !canIndexRoute(_) }
        ).flatten
      }
    )

    // Add constant routes to admin index
    server
      .addAdminRoutes(
        toAdminHttpServerRoutes(adminIndexRoutes, router)
          .map(AdminThreadPoolFilter.isolateRoute)
      )

    // Add rich handler for all other routes
    if (adminRichHandlerRoutes.nonEmpty) {
      HttpMuxer
        .addRichHandler(
          HttpRouter.FinatraAdminPrefix,
          AdminThreadPoolFilter.isolateService(router.services.adminService)
        )
    }
  }

  /* Private */

  /** Check if routes define a RouteIndex but are NOT eligible for TwitterServer HTTP Admin Interface index. */
  private def checkRoutesWithRouteIndex(
    routes: Seq[Route]
  )(predicate: Route => Boolean): Seq[Issue] = {
    routes.filter(route => route.index.isDefined && predicate(route)).map { route =>
      Issue(s""""${route.summary}" specifies a RouteIndex but cannot be added to the index.""")
    }
  }

  /** Allows HTTP methods: GET, POST or AnyMethod (with the assumption that users will only answer GET or POST) */
  private def hasAcceptableAdminIndexRouteMethod(route: Route) = route.method match {
    case Method.Get | Method.Post | AnyMethod => true
    case _ => false
  }

  /** Routes to include in the index MUST start with /admin and have an acceptable HTTP method */
  private def canIndexRoute(route: Route) =
    route.path.startsWith("/admin") && hasAcceptableAdminIndexRouteMethod(route)

  private def toAdminHttpServerRoutes(
    routes: Seq[Route],
    router: HttpRouter
  ): Seq[AdminHttpServer.Route] = {
    routes.map { route =>
      route.index match {
        case Some(index) =>
          AdminHttpServer.mkRoute(
            path = route.path,
            handler = router.services.adminService,
            alias = if (index.alias.nonEmpty) index.alias else route.path,
            group = Some(index.group),
            includeInIndex = canIndexRoute(route),
            method = route.method
          )
        case _ =>
          AdminHttpServer.mkRoute(
            path = route.path,
            handler = router.services.adminService,
            alias = route.path,
            group = None,
            includeInIndex = false,
            method = route.method
          )
      }
    }
  }
}
