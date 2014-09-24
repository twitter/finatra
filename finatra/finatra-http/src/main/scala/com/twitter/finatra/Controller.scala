package com.twitter.finatra

import com.twitter.finatra.marshalling.CallbackConvertor
import com.twitter.finatra.response._
import com.twitter.finatra.twitterserver.routing.Route
import com.twitter.finatra.utils.Logging
import javax.inject.Inject
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._
import scala.collection.mutable.ArrayBuffer

abstract class Controller extends Logging {

  /*
   * Using constructor-injection would complicate client usage,
   * so we use vars here to allow direct class injection
   */
  @Inject private var callbackConvertor: CallbackConvertor = _
  @Inject private var responseBuilder: ResponseBuilder = _

  /* We need to create "lazy routes" first, since the closed over messageBodyManager is injected after object creation :-/ */
  private val routesBeforeInjection = ArrayBuffer[() => Route]()
  private[finatra] lazy val routes = routesBeforeInjection map {_()}

  /* Protected */

  protected def get[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit =
    addRoute(GET, route, callback)

  protected def post[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit =
    addRoute(POST, route, callback)

  protected def put[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit =
    addRoute(PUT, route, callback)

  protected def delete[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit =
    addRoute(DELETE, route, callback)

  protected def options[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit =
    addRoute(OPTIONS, route, callback)

  protected def patch[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => ResponseType): Unit =
    addRoute(PATCH, route, callback)

  /** head's callback returns Unit since HEAD methods must have empty bodies */
  protected def head[RequestType: Manifest, ResponseType: Manifest](route: String)(callback: RequestType => Unit): Unit =
    addRoute(HEAD, route, callback)

  protected def response = responseBuilder

  /* Private */

  private def addRoute[RequestType: Manifest, ResponseType: Manifest](method: HttpMethod, route: String, callback: RequestType => ResponseType) {
    (() =>
      Route(
        method,
        route,
        callbackConvertor.convertToFutureResponse(callback),
        getClass.getDeclaredAnnotations,
        manifest[RequestType].runtimeClass,
        manifest[ResponseType].runtimeClass)) +=: routesBeforeInjection
  }
}
