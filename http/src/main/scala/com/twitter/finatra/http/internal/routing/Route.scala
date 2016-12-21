package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Method, Request, Response, RouteIndex}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.request.RequestWithRouteParams
import com.twitter.util.Future
import java.lang.annotation.Annotation
import scala.language.existentials

//optimized
private[finatra] case class Route(
  name: String,
  method: Method,
  path: String,
  admin: Boolean,
  index: Option[RouteIndex],
  callback: Request => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: Class[_],
  responseClass: Class[_],
  routeFilter: Filter[Request, Response, Request, Response],
  filter: Filter[Request, Response, Request, Response]) {

  private[this] val pattern = PathPattern(path)
  private[this] val routeInfo = RouteInfo(name, path)

  private[this] val callbackService: Service[Request, Response] =
    Service.mk[Request, Response](callback)
  private[this] val filteredCallback: Request => Future[Response] =
    filter.andThen(callbackService)

  /* Public */

  def captureNames = pattern.captureNames

  def constantRoute = captureNames.isEmpty

  def summary: String = f"$method%-7s $path"

  def withFilter(filter: Filter[Request, Response, Request, Response]): Route = {
    this.copy(filter = filter.andThen(this.filter))
  }

  // Note: incomingPath is an optimization to avoid calling request.path for every potential route
  def handle(
    request: Request,
    incomingPath: String,
    bypassFilters: Boolean): Option[Future[Response]] = {
    val routeParamsOpt = pattern.extract(incomingPath)
    if (routeParamsOpt.isEmpty) {
      None
    } else {
      handleMatch(
        createRequest(
          request,
          routeParamsOpt.get),
        bypassFilters)
    }
  }

  def handleMatch(request: Request, bypassFilters: Boolean): Some[Future[Response]] = {
    RouteInfo.set(request, routeInfo)
    if (bypassFilters)
      Some(
        routeFilter.andThen(callbackService).apply(request))
    else
      Some(
        filteredCallback(request))
  }

  /* Private */

  private[this] def createRequest(request: Request, routeParams: Map[String, String]) = {
    if (routeParams.isEmpty)
      request
    else
      new RequestWithRouteParams(request, routeParams)
  }
}
