package com.twitter.finatra.http

import com.twitter.finagle.http.{RouteIndex, Method => HttpMethod}
import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.inject.Injector
import java.lang.annotation.Annotation

private[http] class RouteBuilder[RequestType: Manifest, ResponseType: Manifest](
  method: HttpMethod,
  route: String,
  name: String,
  admin: Boolean,
  index: Option[RouteIndex],
  callback: RequestType => ResponseType,
  annotations: Array[Annotation],
  routeDsl: RouteContext) {

  def build(callbackConverter: CallbackConverter, injector: Injector) = Route(
    name = name,
    method = method,
    uri = route,
    admin = admin,
    index = index,
    callback = callbackConverter.convertToFutureResponse(callback),
    annotations = annotations,
    requestClass = manifest[RequestType].runtimeClass,
    responseClass = manifest[ResponseType].runtimeClass,
    routeFilter = routeDsl.buildFilter(injector),
    filter = routeDsl.buildFilter(injector))
}
