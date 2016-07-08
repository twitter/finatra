package com.twitter.finatra.http

import com.google.inject.Module
import com.twitter.finagle._
import com.twitter.finagle.http.{HttpMuxer, Method, Request, Response}
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.finatra.http.modules._
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Lifecycle
import com.twitter.server.AdminHttpServer

trait HttpServer extends BaseHttpServer {

  addFrameworkModules(
    mustacheModule,
    messageBodyModule,
    exceptionMapperModule,
    exceptionManagerModule,
    jacksonModule,
    DocRootModule,
    accessLogModule,
    Slf4jBridgeModule)

  /* Abstract */

  protected def configureHttp(router: HttpRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()
    val httpRouter = injector.instance[HttpRouter]
    configureHttp(httpRouter)
  }

  /* Overrides */

  override final def httpService: Service[Request, Response] = {
    val router = injector.instance[HttpRouter]
    addAdminRoutes(router)
    router.services.externalService
  }

  /* Protected */

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finagle.filter.LogFormatter]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finagle.filter.LogFormatter]] implementation.
   */
  protected def accessLogModule: Module = AccessLogModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.github.mustachejava.MustacheFactory]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.github.mustachejava.MustacheFactory]] implementation.
   */
  protected def mustacheModule: Module = MustacheModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing implementations for a
   * [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] and a
   * [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides implementations for
   *         [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] and
   *         [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
   */
  protected def messageBodyModule: Module = MessageBodyModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing an implementation for a
   * [[com.twitter.finatra.http.exceptions.DefaultExceptionMapper]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a
   *         [[com.twitter.finatra.http.exceptions.DefaultExceptionMapper]] implementation.
   */
  protected def exceptionMapperModule: Module = ExceptionMapperModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finatra.http.exceptions.ExceptionManager]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a
   *         [[com.twitter.finatra.http.exceptions.ExceptionManager]] implementation.
   */
  protected def exceptionManagerModule: TwitterModule = ExceptionManagerModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finatra.json.FinatraObjectMapper]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finatra.json.FinatraObjectMapper]] implementation.
   */
  protected def jacksonModule: Module = FinatraJacksonModule

  /* Private */

  private def addAdminRoutes(router: HttpRouter): Unit = {
    val allTwitterServerAdminRoutes = this.routes.map(_.path).union(HttpMuxer.patterns)
    val conflicts = allTwitterServerAdminRoutes.intersect(router.routesByType.admin.map(_.path))
    if (conflicts.nonEmpty) {
      val errorMessage = "Adding admin routes with paths that overlap with pre-defined TwitterServer admin route paths is not allowed."
      error(s"$errorMessage \nConflicting route paths: \n\t${conflicts.mkString("\n\t")}")
      throw new Exception(errorMessage)
    }

    // Constant routes that don't start with /admin/finatra can be added to the admin index,
    // all other routes cannot. Only constant /GET routes will be eligible to be added to the admin UI.
    // NOTE: beforeRouting = true filters will not be properly evaluated on adminIndexRoutes
    // since the localMuxer in the AdminHttpServer does exact route matching before marshalling
    // to the handler service (where the filter is composed). Thus if this filter defines a route
    // it will not get routed to by the localMuxer. Any beforeRouting = true filters should act
    // only on paths behind /admin/finatra.
    val (adminIndexRoutes, adminRichHandlerRoutes) = router.routesByType.admin.partition { route =>
      // can't start with /admin/finatra/ and is a constant route
      !route.path.startsWith(HttpRouter.FinatraAdminPrefix) && route.constantRoute
    }

    // check if routes define an AdminIndexInfo but are not eligible for admin UI index
    warnIfRoutesDefineAdminIndexInfo(adminRichHandlerRoutes) { route => true }
    warnIfRoutesDefineAdminIndexInfo(adminIndexRoutes) { route => route.method != Method.Get }

    // Add constant routes to admin index
    addAdminRoutes(
      toAdminHttpServerRoutes(
        adminIndexRoutes, router))

    // Add rich handler for all other routes
    if (adminRichHandlerRoutes.nonEmpty) {
      HttpMuxer.addRichHandler(
        HttpRouter.FinatraAdminPrefix,
        router.services.adminService)
    }
  }

  private def warnIfRoutesDefineAdminIndexInfo(routes: Seq[Route])(predicate: Route => Boolean): Unit = {
    for {
      route <- routes if predicate(route) && route.adminIndexInfo.isDefined
    } {
      warn(s"${route.path} defines an AdminIndexInfo but is not eligible to be added to the admin UI index. " +
        s"Only constant /GET routes that do not start with ${HttpRouter.FinatraAdminPrefix} can be added to the admin UI index.")
    }
  }

  private def toAdminHttpServerRoutes(routes: Seq[Route], router: HttpRouter): Seq[AdminHttpServer.Route] = {
    routes map { route =>
      val includeInIndex = route.adminIndexInfo.isDefined && route.method == Method.Get
      val alias = route.adminIndexInfo match {
        case Some(info) if info.alias.nonEmpty => info.alias
        case _ => route.path
      }
      val group = route.adminIndexInfo.map(_.group).getOrElse("Finatra")
      AdminHttpServer.Route(
        path = route.path,
        handler = router.services.adminService,
        alias = alias,
        group = Some(group),
        includeInIndex = includeInIndex)
    }
  }
}
