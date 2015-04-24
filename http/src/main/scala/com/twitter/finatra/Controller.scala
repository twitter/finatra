package com.twitter.finatra

import com.twitter.finatra.internal.marshalling.CallbackConverter
import com.twitter.finatra.internal.routing.Route
import com.twitter.finatra.response._
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

  protected def get[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(GET, route, callback)
  protected def post[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(POST, route, callback)
  protected def put[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(PUT, route, callback)
  protected def delete[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(DELETE, route, callback)
  protected def options[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(OPTIONS, route, callback)
  protected def patch[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(PATCH, route, callback)
  protected def head[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(HEAD, route, callback)
  protected def connect[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(CONNECT, route, callback)
  protected def trace[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit = addRoute(TRACE, route, callback)

  protected def response = responseBuilder

  /* Private */

  private def addRoute[RequestType: Manifest, ResponseType: Manifest](
    method: HttpMethod,
    route: String,
    callback: RequestType => ResponseType) {
    routesBeforeInjection += (() =>
      Route(
        method,
        route,
        callbackConverter.convertToFutureResponse(callback),
        getClass.getDeclaredAnnotations,
        manifest[RequestType].runtimeClass,
        manifest[ResponseType].runtimeClass))
  }
}
