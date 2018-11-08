package com.twitter.finatra.http

import com.twitter.finagle.http.{Method => HttpMethod, RouteIndex}
import com.twitter.finatra.http.internal.marshalling.CallbackConverter
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.inject.Injector
import java.lang.annotation.Annotation
import scala.reflect.classTag

private[http] class RouteBuilder[RequestType: Manifest, ResponseType: Manifest](
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

  def build(callbackConverter: CallbackConverter, injector: Injector): Route =
    Route(
      name = name,
      method = method,
      uri = route,
      clazz = clazz,
      admin = admin,
      index = index,
      callback = callbackConverter.convertToFutureResponse(callback),
      annotations = annotations,
      requestClass = classTag[RequestType],
      responseClass = classTag[ResponseType],
      routeFilter = routeDsl.buildFilter(injector),
      filter = routeDsl.buildFilter(injector)
    )
}
