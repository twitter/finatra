package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.utils.Logging
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod._

class RoutingService(
  routes: Seq[Route],
  notFoundService: Service[Request, Response] = new NotFoundService)
  extends Service[Request, Response]
  with Logging {

  private val get = Routes.createForMethod(routes, GET)
  private val post = Routes.createForMethod(routes, POST)
  private val put = Routes.createForMethod(routes, PUT)
  private val delete = Routes.createForMethod(routes, DELETE)
  private val options = Routes.createForMethod(routes, OPTIONS)
  private val patch = Routes.createForMethod(routes, PATCH)
  private val head = Routes.createForMethod(routes, HEAD)

  private val routesStr = routes map {_.summary} mkString ", "

  override def apply(request: Request): Future[Response] = {
    (request.method match {
      case GET => get.handle(request)
      case POST => post.handle(request)
      case PUT => put.handle(request)
      case DELETE => delete.handle(request)
      case OPTIONS => options.handle(request)
      case PATCH => patch.handle(request)
      case HEAD => head.handle(request)
    }).getOrElse {
      debug(request + " not found in " + routesStr)
      notFoundService(request)
    }
  }
}
