package com.twitter.finatra.http

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Response, Request}
import com.twitter.inject.Injector
import org.jboss.netty.handler.codec.http.HttpMethod._
import scala.collection.mutable.ArrayBuffer

private[http] trait RouteDSL { self =>

  private type HttpFilter = Filter[Request, Response, Request, Response]

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

  def get[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(GET, route, name, callback, self)
  def post[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(POST, route, name, callback, self)
  def put[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(PUT, route, name, callback, self)
  def delete[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(DELETE, route, name, callback, self)
  def options[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(OPTIONS, route, name, callback, self)
  def patch[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(PATCH, route, name, callback, self)
  def head[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(HEAD, route, name, callback, self)
  def connect[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(CONNECT, route, name, callback, self)
  def trace[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = routeBuilders += new RouteBuilder(TRACE, route, name, callback, self)
}