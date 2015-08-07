package com.twitter.finatra.http

import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.response._
import com.twitter.inject.{Injector, Logging}
import javax.inject.Inject

abstract class Controller extends RouteDSL with Logging {
  /*
   * NOTE: Using constructor-injection for the following fields would add boilerplate to all
   * controllers, so instead we use vars to allow setter injection after object creation
   */
  @Inject private var callbackConverter: CallbackConverter = _
  @Inject private var responseBuilder: ResponseBuilder = _
  @Inject private var injector: Injector = _

  /* Protected */
  protected def response = responseBuilder

  /* Private */
  /* We create "lazy routes" first, since the callbackConverter and injector are injected after object creation */
  private[finatra] lazy val routes = routeBuilders.map {
    _.build(callbackConverter, injector, getClass.getDeclaredAnnotations)
  }.toSeq
}
