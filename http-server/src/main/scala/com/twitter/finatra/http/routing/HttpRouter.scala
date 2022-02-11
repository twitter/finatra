package com.twitter.finatra.http.routing

import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.AbstractController
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.HttpFilter
import com.twitter.finatra.http.exceptions.AbstractExceptionMapper
import com.twitter.finatra.http.exceptions.ExceptionManager
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.exceptions.ExceptionMapperCollection
import com.twitter.finatra.http.internal.routing.CallbackConverterImpl
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.finatra.http.internal.routing.RoutingService
import com.twitter.finatra.http.marshalling.MessageBodyComponent
import com.twitter.finatra.http.marshalling.MessageBodyManager
import com.twitter.inject.Injector
import com.twitter.inject.TypeUtils
import com.twitter.inject.internal.LibraryRegistry
import com.twitter.util.logging.Logger
import java.lang.annotation.{Annotation => JavaAnnotation}
import javax.inject.Inject
import javax.inject.Singleton
import scala.collection.mutable.ArrayBuffer

object HttpRouter {
  val FinatraAdminPrefix = "/admin/finatra/"

  private val logger: Logger = Logger(HttpRouter.getClass)
}

/**
 * Builds "external" and "admin" instances of a [[RoutingService]]
 * which is a subclass of a [[com.twitter.finagle.Service]]. The "external" [[RoutingService]] will
 * be served on the bound external [[com.twitter.finagle.ListeningServer]], the "admin" [[RoutingService]]
 * will be added as endpoints to the [[com.twitter.finagle.http.HttpMuxer]] for serving on the
 * TwitterServer HTTP Admin Interface.
 *
 * @see [[http://twitter.github.io/finatra/user-guide/http/controllers.html#controllers-and-routing]]
 * @see [[http://twitter.github.io/finatra/user-guide/http/controllers.html#admin-paths]]
 * @see [[https://twitter.github.io/twitter-server/Admin.html TwitterServer HTTP Admin Interface]]
 */
@Singleton
class HttpRouter @Inject() (
  injector: Injector,
  callbackConverter: CallbackConverterImpl,
  messageBodyManager: MessageBodyManager,
  exceptionManager: ExceptionManager) {
  import HttpRouter._

  /* Mutable State */
  private[finatra] var maxRequestForwardingDepth: Int = 5
  private[finatra] var globalBeforeRouteMatchingFilter: HttpFilter = Filter.identity
  private[finatra] var globalFilter: HttpFilter = Filter.identity
  private[finatra] val routes = ArrayBuffer[Route]()

  private[finatra] lazy val routesByType = partitionRoutesByType
  private[finatra] lazy val adminRoutingService = new RoutingService(routesByType.admin)
  private[finatra] lazy val externalRoutingService = new RoutingService(routesByType.external)
  private[finatra] lazy val services: Services = {
    Services(
      routesByType,
      adminService = globalBeforeRouteMatchingFilter.andThen(adminRoutingService),
      externalService = globalBeforeRouteMatchingFilter.andThen(externalRoutingService)
    )
  }

  /* Public */

  /**
   * The resultant external (e.g., non-admin routes only) Finagle `Service[-Request, +Response]`
   * created from all added controllers and filters.
   * @return a configured `Service[-Request, +Response]`
   * @note it is '''important''' to only call this method once '''all router configuration''' has
   *       been applied, otherwise the returned `Service[-Request, +Response]` instance will
   *       be incomplete and any subsequent configuration will not be applied due to memoization.
   */
  def externalService: Service[Request, Response] = services.externalService

  /**
   * Allows maximum forwarding depth for a given [[com.twitter.finagle.http.Request]]
   * to be changed
   * @note This maximum forwarding depth is only enforced by finatra services
   * @param depth the max number of times a given request can be forwarded
   */
  def withMaxRequestForwardingDepth(depth: Int): HttpRouter = {
    require(depth > 0, s"Maximum request forwarding depth: $depth, must be greater than zero.")
    maxRequestForwardingDepth = depth
    this
  }

  def exceptionMapper[T <: ExceptionMapper[_]: Manifest]: HttpRouter = {
    exceptionManager.add[T]
    this
  }

  def exceptionMapper[T <: Throwable: Manifest](mapper: ExceptionMapper[T]): HttpRouter = {
    exceptionManager.add[T](mapper)
    this
  }

  def exceptionMapper[T <: Throwable](clazz: Class[_ <: AbstractExceptionMapper[T]]): HttpRouter = {
    val mapperType = TypeUtils.superTypeFromClass(clazz, classOf[ExceptionMapper[_]])
    val throwableType = TypeUtils.singleTypeParam(mapperType)
    exceptionMapper(injector.instance(clazz))(
      Manifest.classType(Class.forName(throwableType.getTypeName))
    )
    this
  }

  def exceptionMapper(mappers: ExceptionMapperCollection): HttpRouter = {
    exceptionManager.add(mappers)
    this
  }

  def register[MBR <: MessageBodyComponent: Manifest]: HttpRouter = {
    messageBodyManager.add[MBR]()
    this
  }

  def register[MBR <: MessageBodyComponent: Manifest, ObjTypeToReadWrite: Manifest]: HttpRouter = {
    messageBodyManager.addExplicit[MBR, ObjTypeToReadWrite]()
    this
  }

  /** Add global filter used for all requests annotated with Annotation Type */
  def filter[FilterType <: HttpFilter: Manifest, Ann <: JavaAnnotation: Manifest]: HttpRouter =
    filter(injector.instance[FilterType, Ann])

  /** Add global filter used for all requests, by default applied AFTER route matching */
  def filter(clazz: Class[_ <: HttpFilter]): HttpRouter =
    filter(injector.instance(clazz))

  /** Add global filter used for all requests, optionally BEFORE route matching */
  def filter(clazz: Class[_ <: HttpFilter], beforeRouting: Boolean): HttpRouter =
    filter(injector.instance(clazz), beforeRouting)

  /** Add global filter used for all requests, by default applied AFTER route matching */
  def filter(typeLiteral: TypeLiteral[_ <: HttpFilter]): HttpRouter =
    filter(injector.instance(Key.get(typeLiteral)))

  /** Add global filter used for all requests, by default applied AFTER route matching */
  def filter(typeLiteral: TypeLiteral[_ <: HttpFilter], annotation: JavaAnnotation): HttpRouter =
    filter(injector.instance(Key.get(typeLiteral, annotation)))

  /** Add global filter used for all requests, optionally BEFORE route matching */
  def filter(typeLiteral: TypeLiteral[_ <: HttpFilter], beforeRouting: Boolean): HttpRouter =
    filter(injector.instance(Key.get(typeLiteral)), beforeRouting)

  /** Add global filter used for all requests, optionally BEFORE route matching */
  def filter(
    typeLiteral: TypeLiteral[_ <: HttpFilter],
    annotation: JavaAnnotation,
    beforeRouting: Boolean
  ): HttpRouter =
    filter(injector.instance(Key.get(typeLiteral, annotation)), beforeRouting)

  /** Add global filter used for all requests, by default applied AFTER route matching */
  def filter[FilterType <: HttpFilter: Manifest]: HttpRouter =
    filter(injector.instance[FilterType])

  /** Add global filter used for all requests, optionally BEFORE route matching */
  def filter[FilterType <: HttpFilter: Manifest](beforeRouting: Boolean): HttpRouter =
    filter(injector.instance[FilterType], beforeRouting)

  /** Add global filter used for all requests, by default applied AFTER route matching */
  def filter(filter: HttpFilter): HttpRouter = {
    assert(routes.isEmpty, "'filter' must be called before 'add'.")
    globalFilter = globalFilter.andThen(filter)
    this
  }

  /** Add global filter used for all requests, optionally BEFORE route matching */
  def filter(filter: HttpFilter, beforeRouting: Boolean): HttpRouter = {
    if (beforeRouting) {
      assert(
        routes.isEmpty && globalFilter == Filter.identity,
        "'filter[T](beforeRouting = true)' must be called before 'filter' or 'add'."
      )
      globalBeforeRouteMatchingFilter = globalBeforeRouteMatchingFilter.andThen(filter)
      this
    } else {
      this.filter(filter)
    }
  }

  def add[C <: Controller: Manifest]: HttpRouter = {
    val controller = injector.instance[C]
    addRoutes(controller)
  }

  def add(clazz: Class[_ <: AbstractController]): HttpRouter = {
    val controller = injector.instance(clazz)
    add(controller)
  }

  def add(controller: Controller): HttpRouter = {
    injector.underlying.injectMembers(controller)
    addRoutes(controller)
  }

  def add(controller: AbstractController): HttpRouter = {
    injector.underlying.injectMembers(controller)
    controller.configureRoutes()
    addRoutes(controller)
  }

  /** Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add(filter: HttpFilter, controller: Controller): HttpRouter = {
    injector.underlying.injectMembers(controller)
    controller match {
      case abstractController: AbstractController =>
        abstractController.configureRoutes()
      case _ => // do nothing
    }
    addRoutes(filter, controller)
  }

  // Generated
  /* Note: If you have more than 10 filters, combine some of them using MergedFilter (@see CommonFilters) */
  /** Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[F1 <: HttpFilter: Manifest, C <: Controller: Manifest]: HttpRouter =
    addFiltered[C](injector.instance[F1])

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(injector.instance(f1Clazz), injector.instance(controllerClazz))

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector.instance(f1Clazz).andThen(injector.instance(f2Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    F5 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
        .andThen(injector.instance[F5])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    f5Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz))
        .andThen(injector.instance(f5Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    F5 <: HttpFilter: Manifest,
    F6 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
        .andThen(injector.instance[F5])
        .andThen(injector.instance[F6])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    f5Clazz: Class[_ <: HttpFilter],
    f6Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz))
        .andThen(injector.instance(f5Clazz))
        .andThen(injector.instance(f6Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    F5 <: HttpFilter: Manifest,
    F6 <: HttpFilter: Manifest,
    F7 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
        .andThen(injector.instance[F5])
        .andThen(injector.instance[F6])
        .andThen(injector.instance[F7])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    f5Clazz: Class[_ <: HttpFilter],
    f6Clazz: Class[_ <: HttpFilter],
    f7Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz))
        .andThen(injector.instance(f5Clazz))
        .andThen(injector.instance(f6Clazz))
        .andThen(injector.instance(f7Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    F5 <: HttpFilter: Manifest,
    F6 <: HttpFilter: Manifest,
    F7 <: HttpFilter: Manifest,
    F8 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
        .andThen(injector.instance[F5])
        .andThen(injector.instance[F6])
        .andThen(injector.instance[F7])
        .andThen(injector.instance[F8])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    f5Clazz: Class[_ <: HttpFilter],
    f6Clazz: Class[_ <: HttpFilter],
    f7Clazz: Class[_ <: HttpFilter],
    f8Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz))
        .andThen(injector.instance(f5Clazz))
        .andThen(injector.instance(f6Clazz))
        .andThen(injector.instance(f7Clazz))
        .andThen(injector.instance(f8Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    F5 <: HttpFilter: Manifest,
    F6 <: HttpFilter: Manifest,
    F7 <: HttpFilter: Manifest,
    F8 <: HttpFilter: Manifest,
    F9 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
        .andThen(injector.instance[F5])
        .andThen(injector.instance[F6])
        .andThen(injector.instance[F7])
        .andThen(injector.instance[F8])
        .andThen(injector.instance[F9])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    f5Clazz: Class[_ <: HttpFilter],
    f6Clazz: Class[_ <: HttpFilter],
    f7Clazz: Class[_ <: HttpFilter],
    f8Clazz: Class[_ <: HttpFilter],
    f9Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz))
        .andThen(injector.instance(f5Clazz))
        .andThen(injector.instance(f6Clazz))
        .andThen(injector.instance(f7Clazz))
        .andThen(injector.instance(f8Clazz))
        .andThen(injector.instance(f9Clazz)),
      injector.instance(controllerClazz)
    )

  /** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */
  def add[
    F1 <: HttpFilter: Manifest,
    F2 <: HttpFilter: Manifest,
    F3 <: HttpFilter: Manifest,
    F4 <: HttpFilter: Manifest,
    F5 <: HttpFilter: Manifest,
    F6 <: HttpFilter: Manifest,
    F7 <: HttpFilter: Manifest,
    F8 <: HttpFilter: Manifest,
    F9 <: HttpFilter: Manifest,
    F10 <: HttpFilter: Manifest,
    C <: Controller: Manifest
  ]: HttpRouter =
    addFiltered[C](
      injector
        .instance[F1]
        .andThen(injector.instance[F2])
        .andThen(injector.instance[F3])
        .andThen(injector.instance[F4])
        .andThen(injector.instance[F5])
        .andThen(injector.instance[F6])
        .andThen(injector.instance[F7])
        .andThen(injector.instance[F8])
        .andThen(injector.instance[F9])
        .andThen(injector.instance[F10])
    )

  /**
   * Add per-controller filter (Note: Per-controller filters only run if the paired controller has a matching route).
   *
   * This is a recommended API for Java users. Scala users should prefer [[Manifest]]-variant of this method.
   */
  def add(
    f1Clazz: Class[_ <: HttpFilter],
    f2Clazz: Class[_ <: HttpFilter],
    f3Clazz: Class[_ <: HttpFilter],
    f4Clazz: Class[_ <: HttpFilter],
    f5Clazz: Class[_ <: HttpFilter],
    f6Clazz: Class[_ <: HttpFilter],
    f7Clazz: Class[_ <: HttpFilter],
    f8Clazz: Class[_ <: HttpFilter],
    f9Clazz: Class[_ <: HttpFilter],
    f10Clazz: Class[_ <: HttpFilter],
    controllerClazz: Class[_ <: AbstractController]
  ): HttpRouter =
    add(
      injector
        .instance(f1Clazz)
        .andThen(injector.instance(f2Clazz))
        .andThen(injector.instance(f3Clazz))
        .andThen(injector.instance(f4Clazz))
        .andThen(injector.instance(f5Clazz))
        .andThen(injector.instance(f6Clazz))
        .andThen(injector.instance(f7Clazz))
        .andThen(injector.instance(f8Clazz))
        .andThen(injector.instance(f9Clazz))
        .andThen(injector.instance(f10Clazz)),
      injector.instance(controllerClazz)
    )

  /* Private */

  private def addFiltered[C <: Controller: Manifest](filter: HttpFilter): HttpRouter = {
    addRoutes(filter, injector.instance[C])
  }

  private def addRoutes(controller: Controller): HttpRouter = {
    routes ++= buildRoutes(controller)
    this
  }

  private def addRoutes(filter: HttpFilter, controller: Controller): HttpRouter = {
    routes ++= buildRoutes(controller, Some(filter))
    this
  }

  private[this] def buildRoutes(
    controller: Controller,
    filter: Option[HttpFilter] = None
  ): Seq[Route] = {
    val routes = (filter match {
      case Some(controllerFilter) =>
        controller.routeBuilders.map(
          _.build(callbackConverter, injector).withFilter(controllerFilter)
        )
      case _ =>
        controller.routeBuilders.map(_.build(callbackConverter, injector))
    }).toSeq

    registerRoutes(routes)
    registerGlobalFilter(globalFilter)
    routes.map(_.withFilter(globalFilter))
  }

  private[this] def registerGlobalFilter(filter: HttpFilter): Unit = {
    if (filter ne Filter.identity) {
      injector
        .instance[LibraryRegistry]
        .withSection("http")
        .put(Seq("filters"), filter.toString)
    }
  }

  private[this] def registerRoutes(routes: Seq[Route]): Unit = {
    val registrar = new Registrar(
      injector
        .instance[LibraryRegistry]
        .withSection("http", "routes"))
    routes.foreach(registrar.register)
  }

  private[finatra] def partitionRoutesByType: RoutesByType = {
    if (routes.nonEmpty) logger.info("Adding routes\n" + routes.map(_.summary).mkString("\n"))
    val (adminRoutes, externalRoutes) = routes.partition { route =>
      route.path.startsWith(FinatraAdminPrefix) || route.admin
    }
    assertAdminRoutes(adminRoutes)
    RoutesByType(external = externalRoutes.toSeq, admin = adminRoutes.toSeq)
  }

  // non-constant routes MUST start with /admin/finatra
  private[this] def assertAdminRoutes(routes: ArrayBuffer[Route]): Unit = {
    val message =
      "Error adding route: %s. Non-constant admin interface routes must start with prefix: " + HttpRouter.FinatraAdminPrefix

    for (route <- routes) {
      if (!route.constantRoute) {
        // non-constant routes MUST start with /admin/finatra/
        if (!route.path.startsWith(HttpRouter.FinatraAdminPrefix)) {
          val msg = message.format(route.path)
          logger.error(msg)
          throw new java.lang.AssertionError(msg)
        }
      }
    }
  }
}
