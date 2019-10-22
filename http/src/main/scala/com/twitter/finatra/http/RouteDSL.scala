package com.twitter.finatra.http

import com.twitter.finagle.Filter
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.{Method, Request, RouteIndex}
import com.twitter.finatra.http.request.AnyMethod
import com.twitter.inject.Injector
import com.twitter.util.Var
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * RouteContext represents the current contextual attributes at a given point within a controller declaration using RouteDSL
 * In other words, a RouteContext provides information relevant to the current state of the RouteDSL declaration.
 *
 * For instance:
 * {{{
 * class MyController extends Controller {
 *   // RouteContext(prefix = "", buildFilter = identity)
 *   get("endpoint") {}
 *
 *   // Filter pushes a context onto the "stack". It is generated using the context at the top of the "stack"
 *   // RouteContext(prefix = "", buildFilter = filterFunc...)
 *   filter[MyFilter].get("filtered") {}
 *
 *   // We've now returned to the initial context
 *   post("endpoint") {}
 *
 *   // Prefix pushes a new context
 *   // RouteContext(prefix = "v1", buildFilter = identity)
 *   prefix("v1") {
 *     get("api") {}
 *
 *     // RouteContext(prefix = "v1", buildFilter = filterFunc...)
 *     filter[MyFilter].post("filteredPost") {}
 *   }
 * }
 * }}}
 *
 * @param prefix The current routing state's path prefix
 * @param buildFilter The current routing state's filter factory function
 */
private[http] case class RouteContext(prefix: String, buildFilter: Injector => HttpFilter)

/* Mutable */
private trait RouteState {
  // We define this constant separately rather than directly as a default value of context
  // so the "contextVar" val does not rely on "context" val
  private[this] val defaultContext =
    RouteContext(prefix = "", buildFilter = scala.Function.const(Filter.identity))

  private[http] val context: RouteContext = defaultContext

  // It is important that contextVar is lazy so that NullPointerExceptions are not thrown
  // due to class linearization / initialization order
  // Because RouteDSLs are declared as the "body" of an implementing trait (i.e. extending RouteDSL)
  // we ensure that no eagerly evaluated "context" val will attempt to access an uninitialized reference to "contextVar"
  private[http] lazy val contextVar = Var(defaultContext)
}

private[http] class FilteredDSL[FilterType <: HttpFilter: Manifest] extends RouteDSL {
  override private[http] val context = {
    val current = contextVar()
    current.copy(buildFilter = getBuildFilterFunc(current.buildFilter))
  }

  def apply(fn: => Unit): Unit = withContext(context)(fn)

  protected def getBuildFilterFunc(
    currentFunc: Injector => HttpFilter
  ): Injector => HttpFilter = { injector: Injector =>
    currentFunc(injector).andThen(injector.instance[FilterType])
  }

  override private[http] def contextWrapper[T](f: => T): T = withContext(context)(f)
}

private[http] class PrefixedDSL(prefix: String) extends RouteDSL {
  require(
    prefix.startsWith("/"),
    s"""Invalid prefix: "$prefix". Prefixes MUST begin with a forward slash (/).""")

  override private[http] val context = {
    val current = contextVar()
    // prefixes by definition will be concatenated to a route (which MUST begin
    // with a leading slash), thus we remove any trailing slash from a given prefix
    current.copy(prefix = current.prefix + prefix.stripSuffix("/"))
  }

  def apply(fn: => Unit): Unit = withContext(context)(fn)

  override private[http] def contextWrapper[T](f: => T): T = withContext(context)(f)
}

private[http] trait RouteDSL extends RouteState { self =>
  private[http] val routeBuilders = ArrayBuffer[RouteBuilder[_, _]]()
  private[http] val annotations = getClass.getDeclaredAnnotations
  private[http] val clazz = getClass

  def filter[FilterType <: HttpFilter : Manifest]: FilteredDSL[FilterType] = contextWrapper {
    mkFilterDSL[FilterType]()
  }

  /*
    Version of filter[FilterType] that takes in a call-by-name
   */
  def filter[FilterType <: HttpFilter : Manifest](f: => Unit): Unit = contextWrapper {
    filter[FilterType].apply(f)
  }

  def filter(next: HttpFilter): FilteredDSL[HttpFilter] = contextWrapper {
    mkFilterDSL(Some(next))
  }

  def prefix(value: String): PrefixedDSL = contextWrapper {
    new PrefixedDSL(value) {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] val clazz = self.clazz
      override private[http] lazy val contextVar = self.contextVar
    }
  }

  /* GET/ */

  def get[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Get, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def get(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    get(route, admin = false, callback)

  protected def get(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    get(route, admin, null, callback)

  protected def get(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    get(route, "", admin, index, callback)

  protected def get(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    get[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* POST/ */

  def post[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Post, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def post(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    post(route, admin = false, callback)

  protected def post(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    post(route, admin, null, callback)

  protected def post(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    post(route, "", admin, index, callback)

  protected def post(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    post[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* PUT/ */

  def put[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Put, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def put(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    put(route, admin = false, callback)

  protected def put(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    put(route, admin, null, callback)

  protected def put(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    put(route, "", admin, index, callback)

  protected def put(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    put[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* HEAD/ */

  def head[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Head, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def head(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    head(route, admin = false, callback)

  protected def head(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    head(route, admin, null, callback)

  protected def head(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    head(route, "", admin, index, callback)

  protected def head(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    head[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* PATCH/ */

  def patch[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Patch, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def patch(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    patch(route, admin = false, callback)

  protected def patch(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    patch(route, admin, null, callback)

  protected def patch(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    patch(route, "", admin, index, callback)

  protected def patch(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    patch[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* DELETE/ */

  def delete[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Delete, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def delete(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    delete(route, admin = false, callback)

  protected def delete(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    delete(route, admin, null, callback)

  protected def delete(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    delete(route, "", admin, index, callback)

  protected def delete(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    delete[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* TRACE/ */

  def trace[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Trace, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def trace(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    trace(route, admin = false, callback)

  protected def trace(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    trace(route, admin, null, callback)

  protected def trace(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    trace(route, "", admin, index, callback)

  protected def trace(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    trace[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* OPTIONS/ */

  def options[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Options, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def options(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    options(route, admin = false, callback)

  protected def options(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    options(route, admin, null, callback)

  protected def options(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    options(route, "", admin, index, callback)

  protected def options(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    options[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* ANY/ */

  def any[RequestType: TypeTag, ResponseType: TypeTag](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(AnyMethod, route, name, admin, index, callback)

  /* For Java compatibility */

  protected def any(
    route: String,
    callback: JavaCallback[Request, Object]
  ): Unit =
    any(route, admin = false, callback)

  protected def any(
    route: String,
    admin: Boolean,
    callback: JavaCallback[Request, Object]
  ): Unit =
    any(route, admin, null, callback)

  protected def any(
    route: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    any(route, "", admin, index, callback)

  protected def any(
    route: String,
    name: String,
    admin: Boolean,
    index: RouteIndex,
    callback: JavaCallback[Request, Object]
  ): Unit =
    any[Request, Object](
      route,
      name,
      admin,
      Option(index)
    )(callback.apply)

  /* Private */

  /* A function that wraps another and sets any contexts, if necessary */
  private[http] def contextWrapper[T](f: => T): T = f

  /* Execute a block with a given RouteContext */
  private[http] def withContext[T](ctx: RouteContext)(f: => T): T = {
    val orig = contextVar()
    contextVar() = ctx
    try {
      f
    } finally {
      contextVar() = orig
    }
  }

  private def add[RequestType : TypeTag, ResponseType : TypeTag](
    method: Method,
    route: String,
    name: String,
    admin: Boolean,
    index: Option[RouteIndex],
    callback: RequestType => ResponseType
  ) = {
    require(
      route.startsWith("/"),
      s"""Invalid route: "$route." Routes MUST begin with a forward slash (/).""")

    contextWrapper {
      routeBuilders += new RouteBuilder(
        method,
        prefixRoute(route),
        name,
        clazz,
        admin,
        index,
        callback,
        annotations,
        contextVar().copy()
      )
    }
  }

  private def prefixRoute(route: String): String = {
    /* routes and prefixes MUST begin with a leading / */
    contextVar().prefix match {
      case prefix if prefix.nonEmpty => s"$prefix$route"
      case _ => route
    }
  }

  private def mkFilterDSL[FilterType <: HttpFilter : Manifest](instance: Option[FilterType] = None) =
    new FilteredDSL[FilterType] {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] val clazz = self.clazz
      override private[http] lazy val contextVar = self.contextVar

      override protected def getBuildFilterFunc(
        currentFunc: Injector => HttpFilter
      ): Injector => HttpFilter = { injector: Injector =>
        instance match {
          case Some(f) => currentFunc(injector).andThen(f)
          case None => super.getBuildFilterFunc(currentFunc)(injector)
        }
      }
    }
}
