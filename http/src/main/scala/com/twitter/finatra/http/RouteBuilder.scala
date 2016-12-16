package com.twitter.finatra.http

import com.twitter.finagle.http.{Method => HttpMethod, RouteIndex}
import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.inject.Injector

private[http] class RouteBuilder[RequestType: Manifest, ResponseType: Manifest](
  method: HttpMethod,
  route: String,
  name: String,
  admin: Boolean,
  index: Option[RouteIndex],
  callback: RequestType => ResponseType,
  routeDsl: RouteDSL) {

  def build(callbackConverter: CallbackConverter, injector: Injector) = Route(
    name = name,
    method = method,
    path = route,
    admin = admin,
    index = index,
    callback = callbackConverter.convertToFutureResponse(callback),
    annotations = routeDsl.annotations,
    requestClass = manifest[RequestType].runtimeClass,
    responseClass = manifest[ResponseType].runtimeClass,
    routeFilter = routeDsl.buildFilter(injector),
    filter = routeDsl.buildFilter(injector))
}
