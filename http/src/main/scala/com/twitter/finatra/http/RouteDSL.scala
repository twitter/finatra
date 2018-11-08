package com.twitter.finatra.http

import com.twitter.finagle.Filter
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http._
import com.twitter.inject.Injector
import com.twitter.util.Var

import scala.collection.mutable.ArrayBuffer

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
    RouteContext(prefix = "", buildFilter = Function.const(Filter.identity))

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

  def filter[FilterType <: HttpFilter: Manifest]: FilteredDSL[FilterType] = contextWrapper {
    new FilteredDSL[FilterType] {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] val clazz = self.clazz
      override private[http] lazy val contextVar = self.contextVar
    }
  }

  def filter(next: HttpFilter): FilteredDSL[HttpFilter] = contextWrapper {
    new FilteredDSL[HttpFilter] {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] val clazz = self.clazz
      override private[http] lazy val contextVar = self.contextVar
      override protected def getBuildFilterFunc(
        currentFunc: Injector => HttpFilter
      ): Injector => HttpFilter = { injector: Injector =>
        currentFunc(injector).andThen(next)
      }
    }
  }

  def prefix(value: String): PrefixedDSL = contextWrapper {
    new PrefixedDSL(value) {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] val clazz = self.clazz
      override private[http] lazy val contextVar = self.contextVar
    }
  }

  private val NoopCallback: Request => Response = _ => response.SimpleResponse(Status.Ok)

  def get[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Get, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def post[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Post, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def put[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Put, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def delete[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Delete, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def options[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None
  )(callback: RequestType => ResponseType): Unit =
    add(Options, route, name, admin, index, callback)

  def patch[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Patch, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def head[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Head, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def trace[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(Trace, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  def any[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None,
    registerOptionsRoute: Boolean = false
  )(callback: RequestType => ResponseType): Unit = {
      add(AnyMethod, route, name, admin, index, callback)
      if (registerOptionsRoute) {
        options(route, name, admin, index)(NoopCallback)
      }
    }

  /* Protected */

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

  /* Private */

  private def add[RequestType: Manifest, ResponseType: Manifest](
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
}
