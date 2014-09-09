package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.request.RouteParams
import com.twitter.finatra.{Request => FinatraRequest}
import com.twitter.util.Future
import java.lang.annotation.Annotation
import org.jboss.netty.handler.codec.http.HttpMethod

case class Route(
  method: HttpMethod,
  path: String,
  callback: FinatraRequest => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: Class[_],
  responseClass: Class[_]) {

  private[this] val pattern = PathPattern(path)

  /* Public */

  lazy val captureNames = pattern.captureNames

  def handle(request: Request): Option[Future[Response]] = {
    if (request.method != method)
      None
    else
      for {
        pathParams <- pattern.extract(request.path)
        finatraRequest = FinatraRequest(request, RouteParams(pathParams))
      } yield callback(finatraRequest)
  }
}
