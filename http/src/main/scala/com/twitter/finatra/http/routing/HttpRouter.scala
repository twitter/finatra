package com.twitter.finatra.http.routing

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.internal.exceptions.ExceptionManager
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.internal.routing.{Route, RoutesByType, RoutingService, Services}
import com.twitter.finatra.http.marshalling.MessageBodyComponent
import com.twitter.inject.{Injector, Logging}
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ArrayBuffer

object HttpRouter {
  val FinatraAdminPrefix = "/admin/finatra/"
}

@Singleton
class HttpRouter @Inject()(
  injector: Injector,
  messageBodyManager: MessageBodyManager,
  exceptionManager: ExceptionManager)
  extends Logging {

  private type HttpFilter = Filter[Request, Response, Request, Response]

  /* Mutable State */
  private[finatra] val globalFilters = ArrayBuffer[HttpFilter](Filter.identity)
  private[finatra] val routes = ArrayBuffer[Route]()

  private[finatra] lazy val services: Services = {
    val routesByType = partitionRoutesByType()
    val composedGlobalFilter = globalFilters reduce {_ andThen _}

    Services(
      routesByType,
      adminService = composedGlobalFilter andThen new RoutingService(routesByType.admin),
      externalService = composedGlobalFilter andThen new RoutingService(routesByType.external))
  }

  /* Public */

  def exceptionMapper[T <: ExceptionMapper[_]: Manifest] = {
    exceptionManager.add[T]
    this
  }

  def exceptionMapper[T <: Throwable : Manifest](mapper: ExceptionMapper[T]) = {
    exceptionManager.add[T](mapper)
    this
  }

  def register[MBR <: MessageBodyComponent : Manifest] = {
    messageBodyManager.add[MBR]()
    this
  }

  def register[MBR <: MessageBodyComponent : Manifest, ObjTypeToReadWrite: Manifest] = {
    messageBodyManager.addExplicit[MBR, ObjTypeToReadWrite]()
    this
  }

  /** Add global filter used for all requests */
  def filter[FilterType <: HttpFilter : Manifest] = {
    globalFilters += injector.instance[FilterType]
    this
  }

  /** Add global filter used for all requests */
  def filter(filter: HttpFilter) = {
    globalFilters += filter
    this
  }

  def add(controller: Controller): HttpRouter = {
    injector.underlying.injectMembers(controller)
    addInjected(controller)
  }

  /** Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add(filter: HttpFilter, controller: Controller): HttpRouter = {
    injector.underlying.injectMembers(controller)
    addInjected(filter, controller)
  }

  def add[C <: Controller : Manifest]: HttpRouter = {
    val controller = injector.instance[C]
    addInjected(controller)
  }

  // Generated
  /* Note: If you have more than 10 filters, combine some of them using MergedFilter (@see CommonFilters) */
  /** Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, F5 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4] andThen injector.instance[F5])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, F5 <: HttpFilter : Manifest, F6 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4] andThen injector.instance[F5] andThen injector.instance[F6])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, F5 <: HttpFilter : Manifest, F6 <: HttpFilter : Manifest, F7 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4] andThen injector.instance[F5] andThen injector.instance[F6] andThen injector.instance[F7])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, F5 <: HttpFilter : Manifest, F6 <: HttpFilter : Manifest, F7 <: HttpFilter : Manifest, F8 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4] andThen injector.instance[F5] andThen injector.instance[F6] andThen injector.instance[F7] andThen injector.instance[F8])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, F5 <: HttpFilter : Manifest, F6 <: HttpFilter : Manifest, F7 <: HttpFilter : Manifest, F8 <: HttpFilter : Manifest, F9 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4] andThen injector.instance[F5] andThen injector.instance[F6] andThen injector.instance[F7] andThen injector.instance[F8] andThen injector.instance[F9])

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter : Manifest, F2 <: HttpFilter : Manifest, F3 <: HttpFilter : Manifest, F4 <: HttpFilter : Manifest, F5 <: HttpFilter : Manifest, F6 <: HttpFilter : Manifest, F7 <: HttpFilter : Manifest, F8 <: HttpFilter : Manifest, F9 <: HttpFilter : Manifest, F10 <: HttpFilter : Manifest, C <: Controller : Manifest]: HttpRouter = add[C](injector.instance[F1] andThen injector.instance[F2] andThen injector.instance[F3] andThen injector.instance[F4] andThen injector.instance[F5] andThen injector.instance[F6] andThen injector.instance[F7] andThen injector.instance[F8] andThen injector.instance[F9] andThen injector.instance[F10])

  /* Private */

  private def add[C <: Controller : Manifest](filter: HttpFilter) = {
    addInjected(
      filter,
      injector.instance[C])
  }

  private def addInjected(controller: Controller) = {
    routes ++= controller.routes
    this
  }

  private def addInjected(filter: HttpFilter, controller: Controller) = {
    val routesWithFilter = controller.routes map {_.withFilter(filter)}
    routes ++= routesWithFilter
    this
  }

  private[finatra] def partitionRoutesByType(): RoutesByType = {
    info("Adding routes\n" + (routes.map {_.summary} mkString "\n"))
    val (adminRoutes, externalRoutes) = routes partition {_.path.startsWith("/admin")}
    assertAdminRoutes(adminRoutes)
    RoutesByType(
      external = externalRoutes.toSeq,
      admin = adminRoutes.toSeq)
  }

  private def assertAdminRoutes(routes: ArrayBuffer[Route]) = {
    for (route <- routes) {
      if (!(route.path startsWith HttpRouter.FinatraAdminPrefix)) {
        throw new java.lang.AssertionError(
          "Error adding " + route.path + ". Finatra /admin routes must start with /admin/finatra/")
      }
    }
  }
}
