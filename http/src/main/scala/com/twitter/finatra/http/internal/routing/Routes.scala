package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import scala.collection._

object Routes {

  def createForMethod(routes: Seq[Route], method: HttpMethod) = {
    Routes(
      (routes filter {_.method == method}).toIndexedSeq)
  }
}

case class Routes(
  routes: Seq[Route]) {

  //optimized: Request#path is not internally memoized
  def handle(request: Request): Option[Future[Response]] = {
    val path = request.path
    for (route <- routes) {
      val responseOpt = route.handle(request, path)
      if (responseOpt.isDefined) {
        return responseOpt
      }
    }
    None
  }
}
