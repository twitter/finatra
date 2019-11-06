package com.twitter.finatra.http

import com.twitter.finagle.http.{RouteIndex, Method => HttpMethod}
import com.twitter.finatra.http.internal.routing.{CallbackConverter, Route}
import com.twitter.inject.{Injector, TypeUtils}
import java.lang.annotation.Annotation
import scala.reflect.classTag
import scala.reflect.runtime.universe._

private[http] class RouteBuilder[RequestType: TypeTag, ResponseType: TypeTag](
  method: HttpMethod,
  route: String,
  name: String,
  clazz: Class[_],
  admin: Boolean,
  index: Option[RouteIndex],
  callback: RequestType => ResponseType,
  annotations: Array[Annotation],
  routeDsl: RouteContext
) {

  def build(callbackConverter: CallbackConverter, injector: Injector) =
    Route(
      name = name,
      method = method,
      uri = route,
      clazz = clazz,
      admin = admin,
      index = index,
      callback = callbackConverter.convertToFutureResponse(callback),
      annotations = annotations,
      requestClass = classTag(TypeUtils.asManifest[RequestType]),
      responseClass = classTag(TypeUtils.asManifest[ResponseType]),
      routeFilter = routeDsl.buildFilter(injector),
      filter = routeDsl.buildFilter(injector)
    )
}
