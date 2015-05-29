package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.http.internal.contexts.RouteInfo
import com.twitter.finatra.http.internal.request.RequestWithPathParams
import com.twitter.util.Future
import java.lang.annotation.Annotation
import org.jboss.netty.handler.codec.http.HttpMethod
import scala.language.existentials

//optimized
case class Route(
  name: String,
  method: HttpMethod,
  path: String,
  callback: Request => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: Class[_],
  responseClass: Class[_],
  filter: Filter[Request, Response, Request, Response] = Filter.identity) {

  private val pattern = PathPattern(path)
  private val service = Service.mk[Request, Response](callback)
  private val routeInfo = RouteInfo(name, method)

  /* Public */

  def captureNames: Seq[String] = pattern.captureNames

  def hasEmptyCaptureNames = captureNames.isEmpty

  def summary: String = f"$method%-7s $path"

  def withFilter(filter: Filter[Request, Response, Request, Response]): Route = {
    this.copy(filter = filter)
  }

  // Note: incomingPath is an optimization to avoid calling incomingRequest.path for every potential route
  def handle(incomingRequest: Request, incomingPath: String): Option[Future[Response]] = {
    if (incomingRequest.method != method) {
      None
    }
    else {
      for {
        pathParams <- pattern.extract(incomingPath)
        request = createRequest(incomingRequest, pathParams)
      } yield {
        RouteInfo.set(request, routeInfo)
        filter(request, service)
      }
    }
  }

  /* Private */

  private def createRequest(request: Request, pathParams: Map[String, String]) = {
    if (pathParams.isEmpty)
      request
    else
      new RequestWithPathParams(request, pathParams)
  }
}
