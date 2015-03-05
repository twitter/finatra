package com.twitter.finatra.benchmarks.alternatives

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.internal.request.RequestWithPathParams
import com.twitter.finatra.internal.routing.PathPattern
import com.twitter.util.Future
import java.lang.annotation.Annotation
import org.jboss.netty.handler.codec.http.HttpMethod

//optimized
case class Route2NoPrivateThis(
  method: HttpMethod,
  path: String,
  callback: Request => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  filterOpt: Option[Filter[Request, Response, Request, Response]] = None) {

  private val pattern = PathPattern(path)

  private lazy val filter = filterOpt.get

  private val service = Service.mk[Request, Response](callback)

  /* Public */

  def captureNames: Seq[String] = pattern.captureNames

  def hasEmptyCaptureNames = captureNames.isEmpty

  def summary: String = method + " " + path

  def withFilter(filter: Filter[Request, Response, Request, Response]): Route2NoPrivateThis = {
    this.copy(filterOpt = Some(filter))
  }

  // Note: incomingPath is an optimization to avoid calling incomingRequest.path for every potential Route2
  def handle(incomingRequest: Request, incomingPath: String): Option[Future[Response]] = {
    if (incomingRequest.method != method) {
      None
    }
    else {
      for {
        pathParams <- pattern.extract(incomingPath)
        request = createRequest(incomingRequest, pathParams)
      } yield callService(request)
    }
  }

  /* Private */

  private def createRequest(request: Request, pathParams: Map[String, String]) = {
    if (pathParams.isEmpty)
      request
    else
      new RequestWithPathParams(request, pathParams)
  }

  private def callService(request: Request): Future[Response] = {
    if (filterOpt.isDefined)
      filter(request, service)
    else
      service(request)
  }
}
