package com.twitter.finatra.http

import com.twitter.finagle.Filter
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.{Method, RouteIndex}
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
private[http] case class RouteContext(prefix: String, buildFilter: (Injector) => HttpFilter)

private trait MutableRouteState {
  // We define this constant separately rather than as a directly as a default value of context
  // so the "contextRef" val does not rely on "context" val
  private[this] val defaultContext = RouteContext(
    prefix = "",
    buildFilter = Function.const(Filter.identity)
  )

  private[http] val context: RouteContext = defaultContext

  // It is important that contextRef is lazy so that NullPointerExceptions are not thrown
  // due to class linearization / initialization order
  // Because RouteDSLs are declared as the "body" of an implementing trait (i.e. extending RouteDSL)
  // we ensure that no eagerly evaluated "context" val will attempt to access an uninitialized reference to "contextRef"
  private[http] lazy val contextRef = Var(defaultContext)
}

private[http] class FilteredDSL[FilterType <: HttpFilter : Manifest] extends RouteDSL {
  override private[http] val context = {
    val curr = contextRef()
    curr.copy(buildFilter = getBuildFilterFunc(curr.buildFilter))
  }

  def apply(fn: => Unit): Unit = withContext(context)(fn)

  protected def getBuildFilterFunc(currFunc: (Injector) => HttpFilter): (Injector) => HttpFilter = {
    (injector: Injector) => currFunc(injector).andThen(injector.instance[FilterType])
  }

  override private[http] def contextWrapper[T](f: => T): T = withContext(context)(f)
}

private[http] class PrefixedDSL(prefix: String) extends RouteDSL {
  override private[http] val context = {
    val curr = contextRef()
    curr.copy(prefix = curr.prefix + prefix)
  }

  def apply(fn: => Unit): Unit = withContext(context)(fn)

  override private[http] def contextWrapper[T](f: => T): T = withContext(context)(f)
}

private[http] trait RouteDSL extends MutableRouteState { self =>
  private[http] val routeBuilders = ArrayBuffer[RouteBuilder[_, _]]()
  private[http] val annotations = getClass.getDeclaredAnnotations

  def filter[FilterType <: HttpFilter : Manifest]: FilteredDSL[FilterType] = contextWrapper {
    new FilteredDSL[FilterType] {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] lazy val contextRef = self.contextRef
    }
  }

  def filter(next: HttpFilter): FilteredDSL[HttpFilter] = contextWrapper {
    new FilteredDSL[HttpFilter] {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] lazy val contextRef = self.contextRef
      override protected def getBuildFilterFunc(currFunc: (Injector) => HttpFilter): (Injector) => HttpFilter = {
        (injector: Injector) => currFunc(injector).andThen(next)
      }
    }
  }

  def prefix(value: String): PrefixedDSL = contextWrapper {
    new PrefixedDSL(value) {
      override private[http] val routeBuilders = self.routeBuilders
      override private[http] val annotations = self.annotations
      override private[http] lazy val contextRef = self.contextRef
    }
  }

  def get[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Get, route, name, admin, index, callback)

  def post[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Post, route, name, admin, index, callback)

  def put[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Put, route, name, admin, index, callback)

  def delete[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Delete, route, name, admin, index, callback)

  def options[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Options, route, name, admin, index, callback)

  def patch[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Patch, route, name, admin, index, callback)

  def head[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Head, route, name, admin, index, callback)

  def trace[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(Trace, route, name, admin, index, callback)

  def any[RequestType: Manifest, ResponseType: Manifest](
    route: String,
    name: String = "",
    admin: Boolean = false,
    index: Option[RouteIndex] = None)(callback: RequestType => ResponseType): Unit =
    add(AnyMethod, route, name, admin, index, callback)

  /* Protected */

  // A function that wraps another and sets any contexts, if necessary
  private[http] def contextWrapper[T](f: => T): T = f

  // Executes a block with a given RouteContext
  private[http] def withContext[T](ctx: RouteContext)(f: => T): T = {
    val orig = contextRef()
    contextRef() = ctx
    try f finally contextRef() = orig
  }

  /* Private */

  private def add[RequestType: Manifest, ResponseType: Manifest](
    method: Method,
    route: String,
    name: String,
    admin: Boolean,
    index: Option[RouteIndex],
    callback: RequestType => ResponseType) = contextWrapper {
    routeBuilders += new RouteBuilder(method, prefixRoute(route), name, admin, index, callback, annotations, contextRef().copy())
  }

  private def prefixRoute(route: String): String = {
    contextRef().prefix match {
      case prefix if prefix.nonEmpty && prefix.startsWith("/") => s"$prefix$route"
      case prefix if prefix.nonEmpty && !prefix.startsWith("/") => s"/$prefix$route"
      case _ => route
    }
  }
}
