package com.twitter.finatra.twitterserver.routing

import com.google.inject.Injector
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.Controller
import com.twitter.finatra.marshalling.{MessageBodyComponent, MessageBodyManager}
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import scala.collection.mutable.ArrayBuffer

@Singleton
class Router @Inject()(
  _injector: Injector,
  messageBodyManager: MessageBodyManager) {

  type HttpFilter = Filter[Request, Response, Request, Response]
  private val injector = new ScalaInjector(_injector)

  /* Mutable State */
  private[finatra] val internalFilters = ArrayBuffer[HttpFilter](Filter.identity)
  private[finatra] val externalFilters = ArrayBuffer[HttpFilter](Filter.identity)
  private[finatra] val controllers = ArrayBuffer[Controller]()
  private[finatra] var externalNotFoundService: Service[Request, Response] = new NotFoundService

  private[finatra] lazy val services: Services = {
    val (adminRoutes, externalRoutes) = partitionedRoutes()

    val adminService = {
      val combinedFilters = internalFilters reduce {_ andThen _}
      combinedFilters andThen new RoutingController(adminRoutes)
    }

    val externalService = {
      val combinedExternalFilters = externalFilters reduce {_ andThen _}
      combinedExternalFilters andThen new RoutingController(externalRoutes, externalNotFoundService)
    }

    Services(
      adminRoutes,
      externalRoutes,
      adminService = adminService,
      externalService = externalService)
  }

  /* Public */

  def filter[FilterType <: HttpFilter : Manifest] = {
    // shared filters are added to both filters and external filters
    internalFilters += injector.instance[FilterType]
    externalFilters += injector.instance[FilterType]
    this
  }

  def filter(filter: HttpFilter) = {
    // shared filters are added to both filters and external filters
    internalFilters += filter
    externalFilters += filter
    this
  }

  def internalFilter[FilterType <: HttpFilter : Manifest] = {
    internalFilters += injector.instance[FilterType]
    this
  }

  def internalFilter(filter: HttpFilter) = {
    internalFilters += filter
    this
  }

  def externalFilter[FilterType <: HttpFilter : Manifest] = {
    externalFilters += injector.instance[FilterType]
    this
  }

  def externalFilter(filter: HttpFilter) = {
    externalFilters += filter
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
    controllers += injector.instance[T]
    this
  }

  def add(controller: Controller) = {
    _injector.injectMembers(controller)
    controllers += controller
    this
  }

  def externalNotFoundService(service: Service[Request, Response]): Router = {
    externalNotFoundService = service
    this
  }

  /* Private */

  /** Returns (adminRoutes, externalRoutes */
  private[finatra] def partitionedRoutes(): (Seq[Route], Seq[Route]) = {
    findAllRoutes partition {_.path.startsWith("/admin")}
  }

  private def findAllRoutes = {
    controllers.toSeq flatMap {_.routes}
  }
}
