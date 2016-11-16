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
    name,
    method,
    route,
    admin,
    index,
    callbackConverter.convertToFutureResponse(callback),
    routeDsl.annotations,
    manifest[RequestType].runtimeClass,
    manifest[ResponseType].runtimeClass,
    routeDsl.buildFilter(injector))
}
