package com.twitter.finatra.http

import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.finatra.http.response._
import com.twitter.inject.Logging
import javax.inject.Inject
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._
import scala.collection.mutable.ArrayBuffer

abstract class Controller extends Logging {
  /*
   * NOTE: Using constructor-injection for the following fields would add boilerplate to all
   * controllers, so instead we use vars to allow setter injection after object creation
   */
  @Inject private var callbackConverter: CallbackConverter = _
  @Inject private var responseBuilder: ResponseBuilder = _

  /* We create "lazy routes" first, since the closed over callbackConverter is injected after object creation */
  private val routesBeforeInjection = ArrayBuffer[() => Route]()
  private[finatra] lazy val routes = (routesBeforeInjection map {_()}).toSeq

  /* Protected */

  protected def get[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(GET, route, name, callback)
  protected def post[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(POST, route, name, callback)
  protected def put[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(PUT, route, name, callback)
  protected def delete[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(DELETE, route, name, callback)
  protected def options[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(OPTIONS, route, name, callback)
  protected def patch[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(PATCH, route, name, callback)
  protected def head[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(HEAD, route, name, callback)
  protected def connect[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(CONNECT, route, name, callback)
  protected def trace[RequestType: Manifest, ResponseType: Manifest](route: String, name: String = "")(callback: RequestType => ResponseType): Unit = addRoute(TRACE, route, name, callback)

  protected def response = responseBuilder

  /* Private */

  private def addRoute[RequestType: Manifest, ResponseType: Manifest](
    method: HttpMethod,
    route: String,
    name: String,
    callback: RequestType => ResponseType) {
    routesBeforeInjection += (() =>
      Route(
        name,
        method,
        route,
        callbackConverter.convertToFutureResponse(callback),
        getClass.getDeclaredAnnotations,
        manifest[RequestType].runtimeClass,
        manifest[ResponseType].runtimeClass))
  }
}
