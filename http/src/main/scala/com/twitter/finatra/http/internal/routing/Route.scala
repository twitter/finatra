package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.request.RequestWithPathParams
import com.twitter.util.Future
import java.lang.annotation.Annotation
import scala.language.existentials

//optimized
case class Route(
  name: String,
  method: Method,
  path: String,
  callback: Request => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: Class[_],
  responseClass: Class[_],
  filter: Filter[Request, Response, Request, Response]) {

  private[this] val pattern = PathPattern(path)
  private[this] val routeInfo = RouteInfo(name, path)

  private[this] val filteredCallback: Request => Future[Response] =
    filter andThen Service.mk[Request, Response](callback)

  /* Public */

  def captureNames = pattern.captureNames

  def constantRoute = captureNames.isEmpty

  def summary: String = f"$method%-7s $path"

  def withFilter(filter: Filter[Request, Response, Request, Response]): Route = {
    this.copy(filter = filter andThen this.filter)
  }

  // Note: incomingPath is an optimization to avoid calling incomingRequest.path for every potential route
  def handle(incomingRequest: Request, incomingPath: String): Option[Future[Response]] = {
    val pathParamsOpt = pattern.extract(incomingPath)
    if (pathParamsOpt.isEmpty) {
      None
    }
    else {
      handleMatch(
        createRequest(
          incomingRequest,
          pathParamsOpt.get))
    }
  }

  def handleMatch(request: Request): Some[Future[Response]] = {
    RouteInfo.set(request, routeInfo)
    Some(
      filteredCallback(request))
  }

  /* Private */

  private[this] def createRequest(request: Request, pathParams: Map[String, String]) = {
    if (pathParams.isEmpty)
      request
    else
      new RequestWithPathParams(request, pathParams)
  }
}
