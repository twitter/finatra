package com.twitter.finatra.http

import com.twitter.finagle.Filter
import com.twitter.finagle.http.Method._
import com.twitter.finatra.http.routing.AdminIndexInfo
import com.twitter.inject.Injector
import scala.collection.mutable.ArrayBuffer

private[http] trait RouteDSL { self =>

  private[http] val routeBuilders = ArrayBuffer[RouteBuilder[_, _]]()
  private[http] val annotations = getClass.getDeclaredAnnotations

  private[http] def buildFilter(injector: Injector): HttpFilter = Filter.identity

  protected def filter[FilterType <: HttpFilter : Manifest] = new RouteDSL {
    override val routeBuilders = self.routeBuilders
    override val annotations = self.annotations
    override def buildFilter(injector: Injector) = self.buildFilter(injector).andThen(injector.instance[FilterType])
  }

  def filter(next: HttpFilter) = new RouteDSL {
    override val routeBuilders = self.routeBuilders
    override def buildFilter(injector: Injector) = self.buildFilter(injector).andThen(next)
  }

  def get[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Get, route, name, admin, adminIndexInfo, callback, self)
  def post[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Post, route, name, admin, adminIndexInfo, callback, self)
  def put[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Put, route, name, admin, adminIndexInfo, callback, self)
  def delete[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Delete, route, name, admin, adminIndexInfo, callback, self)
  def options[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Options, route, name, admin, adminIndexInfo, callback, self)
  def patch[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Patch, route, name, admin, adminIndexInfo, callback, self)
  def head[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Head, route, name, admin, adminIndexInfo, callback, self)
  def trace[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(Trace, route, name, admin, adminIndexInfo, callback, self)
  def any[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "", admin: Boolean = false, adminIndexInfo: Option[AdminIndexInfo] = None)(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(AnyMethod, route, name, admin, adminIndexInfo, callback, self)
}
