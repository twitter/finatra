package com.twitter.finatra.http

import com.twitter.finagle.Filter
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.{Method, RouteIndex}
import com.twitter.inject.Injector
import scala.collection.mutable.ArrayBuffer

private[http] trait RouteDSL { self =>

  private[http] val routeBuilders = ArrayBuffer[RouteBuilder[_, _]]()
  private[http] val annotations = getClass.getDeclaredAnnotations
  /* Mutable State */
  private[this] var routePrefix = ""

  private[http] def buildFilter(injector: Injector): HttpFilter = Filter.identity

  def filter[FilterType <: HttpFilter : Manifest] = new RouteDSL {
    override val routeBuilders = self.routeBuilders
    override val annotations = self.annotations
    override def buildFilter(injector: Injector) = self.buildFilter(injector).andThen(injector.instance[FilterType])
  }

  def filter(next: HttpFilter) = new RouteDSL {
    override val routeBuilders = self.routeBuilders
    override def buildFilter(injector: Injector) = self.buildFilter(injector).andThen(next)
  }

  def prefix(value: String)(fn: => Unit): Unit = {
    val previous = routePrefix
    routePrefix = value
    try {
      fn
    } finally {
      routePrefix = previous
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

  /* Private */

  private def add[RequestType: Manifest, ResponseType: Manifest](
    method: Method,
    route: String,
    name: String,
    admin: Boolean,
    index: Option[RouteIndex],
    callback: RequestType => ResponseType) = {
    routeBuilders += new RouteBuilder(method, prefixRoute(route), name, admin, index, callback, self)
  }

  private def prefixRoute(route: String): String = {
    routePrefix match {
      case prefix if prefix.nonEmpty && prefix.startsWith("/") => s"$prefix$route"
      case prefix if prefix.nonEmpty && !prefix.startsWith("/") => s"/$prefix$route"
      case _ => route
    }
  }
}
