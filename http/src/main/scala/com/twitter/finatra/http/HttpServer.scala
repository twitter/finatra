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
import com.twitter.server.AdminHttpServer

trait HttpServer extends BaseHttpServer {

  addFrameworkModules(
    mustacheModule,
    messageBodyModule,
    exceptionMapperModule,
    jacksonModule,
    DocRootModule,
    accessLogModule,
    Slf4jBridgeModule)

  /* Abstract */

  protected def configureHttp(router: HttpRouter): Unit

  /* Overrides */

  override protected def failfastOnFlagsNotParsed = true

  override protected def postStartup() {
    super.postStartup()
    val httpRouter = injector.instance[HttpRouter]
    configureHttp(httpRouter)
  }

  override def httpService: Service[Request, Response] = {
    val router = injector.instance[HttpRouter]
    addAdminRoutes(router)
    router.services.externalService
  }

  /* Protected */

  protected def addAdminRoutes(router: HttpRouter) {
    val allTwitterServerAdminRoutes = this.routes.map(_.path).union(HttpMuxer.patterns)
    val conflicts = allTwitterServerAdminRoutes.intersect(router.routesByType.admin.map(_.path))
    if (conflicts.nonEmpty) {
      val errorMessage = "Adding admin routes with paths that overlap with pre-defined TwitterServer admin route paths is not allowed."
      error(s"$errorMessage \nConflicting route paths: \n\t${conflicts.mkString("\n\t")}")
      throw new Exception(errorMessage)
    }

    // constant /GET routes only can be added to the UI index, all other routes cannot
    val (constantGetRoutes, otherRoutes) = router.routesByType.admin.partition { route =>
      route.constantRoute && route.method == Method.Get
    }
    for (route <- otherRoutes) {
      if (route.adminIndexInfo.isDefined) {
        warn(s"${route.path} defines an AdminIndexInfo but is not a constant /GET route. " +
          "Only constant /GET routes can be added to the admin UI index.")
      }
    }

    // Add constant /GET routes to admin index
    addAdminRoutes(
      toAdminHttpServerRoutes(
        constantGetRoutes, router))

    // Add rich handler for all other routes
    if (otherRoutes.nonEmpty) {
      // Add rich handler for non-constant/non-GET routes with finatra admin prefix to HttpMuxer
      HttpMuxer.addRichHandler(
        HttpRouter.FinatraAdminPrefix,
        router.services.adminService)
    }
  }

  //Note: After upgrading to Guice v4, replace the need for these protected methods with OptionalBinder
  //http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/multibindings/OptionalBinder.html
  protected def accessLogModule: Module = AccessLogModule

  protected def mustacheModule: Module = MustacheModule

  protected def messageBodyModule: Module = MessageBodyModule

  protected def exceptionMapperModule: Module = ExceptionMapperModule

  protected def jacksonModule: Module = FinatraJacksonModule


  /* Private */

  private def toAdminHttpServerRoutes(routes: Seq[Route], router: HttpRouter): Seq[AdminHttpServer.Route] = {
    routes map { route =>
      val includeInIndex = route.adminIndexInfo.isDefined
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
