package com.twitter.finatra.routing

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.Controller
import com.twitter.finatra.guice.FinatraInjector
import com.twitter.finatra.marshalling.{MessageBodyComponent, MessageBodyManager}
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ArrayBuffer

//TODO: Refactor
@Singleton
class Router @Inject()(
  injector: FinatraInjector,
  messageBodyManager: MessageBodyManager) {

  private type HttpFilter = Filter[Request, Response, Request, Response]

  /* Mutable State */
  private[finatra] val commonFilters = ArrayBuffer[HttpFilter](Filter.identity)
  private[finatra] val perControllerFilters = ArrayBuffer[HttpFilter](Filter.identity)
  private[finatra] val routes = ArrayBuffer[Route]()

  private[finatra] lazy val services: Services = {
    val routesByType = partitionRoutesByType()
    val commonCombinedFilter = commonFilters reduce {_ andThen _}

    Services(
      routesByType,
      adminService = commonCombinedFilter andThen new RoutingService(routesByType.admin),
      externalService = commonCombinedFilter andThen new RoutingService(routesByType.external))
  }

  /* Public */

  /**
   * Common filters are used for all controllers
   */
  def commonFilter[FilterType <: HttpFilter : Manifest] = {
    commonFilters += injector.instance[FilterType]
    this
  }

  /**
   * Common filters are used for all controllers
   */
  def commonFilter(filter: HttpFilter) = {
    commonFilters += filter
    this
  }

  /**
   * Non-common filters are only used for the next added controller.
   */
  def filter[FilterType <: HttpFilter : Manifest] = {
    perControllerFilters += injector.instance[FilterType]
    this
  }

  /**
   * Non-common filters are only used for the next added controller.
   */
  def filter(filter: HttpFilter) = {
    perControllerFilters += filter
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

  def add[T <: Controller : Manifest] = {
    val controller = injector.instance[T]
    addInjectedController(controller)
  }

  def add(controller: Controller) = {
    injector.underlying.injectMembers(controller)
    addInjectedController(controller)
  }

  /* Private */

  private[finatra] def partitionRoutesByType(): RoutesByType = {
    val (adminRoutes, externalRoutes) = routes partition {_.path.startsWith("/admin")}
    RoutesByType(
      external = externalRoutes.toSeq,
      admin = adminRoutes.toSeq)
  }

  private def addInjectedController(controller: Controller): Router = {
    if (perControllerFilters.isEmpty) {
      routes ++= controller.routes
    }
    else {
      val combinedPerControllerFilter = perControllerFilters reduce {_ andThen _}
      perControllerFilters.clear()

      val controllerRoutesWithFilter = controller.routes map {_.withFilter(combinedPerControllerFilter)}
      routes ++= controllerRoutesWithFilter
    }

    this
  }
}
