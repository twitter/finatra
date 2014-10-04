package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.request.RouteParams
import com.twitter.finatra.twitterserver.routing.Route.regexChars
import com.twitter.finatra.{Request => FinatraRequest}
import com.twitter.util.Future
import java.lang.annotation.Annotation
import org.apache.commons.lang.StringUtils._
import org.jboss.netty.handler.codec.http.HttpMethod

object Route {
  private val regexChars = Array('?', '[', ']', '\\', '^', '$', '{', '}', '*')
}

case class Route(
  method: HttpMethod,
  path: String,
  callback: FinatraRequest => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: Class[_],
  responseClass: Class[_]) {

  private[this] val pattern = PathPattern(path)

  /* Public */

  val hasConstantPath = {
    pattern.captureNames.isEmpty &&
      containsNone(path, regexChars)
  }

  def captureNames = pattern.captureNames

  def summary = method + " " + path

  def handle(request: Request): Option[Future[Response]] = {
    if (request.method != method)
      None
    else
      for {
        pathParams <- pattern.extract(request.path)
        finatraRequest = FinatraRequest(request, RouteParams.create(pathParams))
      } yield callback(finatraRequest)
  }
}
