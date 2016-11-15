package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.request.RequestWithRouteParams
import com.twitter.finatra.http.routing.{AdminIndexInfo, Prefix}
import com.twitter.util.Future
import java.lang.annotation.Annotation
import scala.language.existentials

//optimized
private[finatra] case class Route(
  name: String,
  method: Method,
  prefix: Prefix = Prefix.empty,
  private val path: String,
  admin: Boolean,
  adminIndexInfo: Option[AdminIndexInfo],
  callback: Request => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: Class[_],
  responseClass: Class[_],
  filter: Filter[Request, Response, Request, Response]) {

  val fullPath: String = prefix(path)
  private[this] val pattern = PathPattern(fullPath)
  private[this] val routeInfo = RouteInfo(name, fullPath)

  private[this] val filteredCallback: Request => Future[Response] =
    filter andThen Service.mk[Request, Response](callback)

  /* Public */

  def captureNames = pattern.captureNames

  def constantRoute = captureNames.isEmpty

  def summary: String = f"$method%-7s $fullPath"

  def withFilter(filter: Filter[Request, Response, Request, Response]): Route = {
    this.copy(filter = filter andThen this.filter)
  }

  // Note: incomingPath is an optimization to avoid calling incomingRequest.path for every potential route
  def handle(incomingRequest: Request, incomingPath: String): Option[Future[Response]] = {
    val routeParamsOpt = pattern.extract(incomingPath)
    if (routeParamsOpt.isEmpty) {
      None
    }
    else {
      handleMatch(
        createRequest(
          incomingRequest,
          routeParamsOpt.get))
    }
  }

  def handleMatch(request: Request): Some[Future[Response]] = {
    RouteInfo.set(request, routeInfo)
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
