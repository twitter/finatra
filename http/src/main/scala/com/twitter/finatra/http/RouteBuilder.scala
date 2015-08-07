package com.twitter.finatra.http

import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.inject.Injector
import java.lang.annotation.Annotation
import org.jboss.netty.handler.codec.http.HttpMethod

private[http] class RouteBuilder[RequestType: Manifest, ResponseType: Manifest](
  method: HttpMethod,
  route: String,
  name: String,
  callback: RequestType => ResponseType,
  routeDsl: RouteDSL) {

  def build(callbackConverter: CallbackConverter, injector: Injector, annotations: Array[Annotation]) = Route(
    name,
    method,
    route,
    callbackConverter.convertToFutureResponse(callback),
    annotations,
    manifest[RequestType].runtimeClass,
    manifest[ResponseType].runtimeClass,
    routeDsl.buildFilter(injector))
}