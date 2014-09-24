package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.conversions.seq._
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import scala.collection._

object Routes {

  def createForMethod(routes: Seq[Route], method: HttpMethod) = {
    val (constantRoutes, nonConstantRoutes) = {
      val methodRoutes = routes filter {_.method == method}
      methodRoutes partition {_.hasConstantPath}
    }

    Routes(
      nonConstantRoutes = nonConstantRoutes,
      constantRoutesMap = constantRoutes groupBySingleValue {_.path})
  }
}

case class Routes(
  nonConstantRoutes: Seq[Route],
  constantRoutesMap: Map[String, Route]) {

  private[this] val nonConstantRoutesArray = nonConstantRoutes.toArray

  /* Public */

  def handle(request: Request): Option[Future[Response]] = {
    constantRoutesMap.get(request.path) match {
      case Some(constantRoute) => constantRoute.handle(request)
      case _ => findRoute(request)
    }
  }

  /* Private */

  private def findRoute(request: Request): Option[Future[Response]] = {
    for (route <- nonConstantRoutesArray) {
      val responseOpt = route.handle(request)
      if (responseOpt.isDefined) {
        return responseOpt
      }
    }
    None
  }
}