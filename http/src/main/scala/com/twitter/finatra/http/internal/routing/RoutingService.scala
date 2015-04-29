package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.inject.Logging
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpMethod._

class RoutingService(
  routes: Seq[Route])
  extends Service[Request, Response]
  with Logging {

  private val get = Routes.createForMethod(routes, GET)
  private val post = Routes.createForMethod(routes, POST)
  private val put = Routes.createForMethod(routes, PUT)
  private val delete = Routes.createForMethod(routes, DELETE)
  private val options = Routes.createForMethod(routes, OPTIONS)
  private val patch = Routes.createForMethod(routes, PATCH)
  private val head = Routes.createForMethod(routes, HEAD)
  private val connect = Routes.createForMethod(routes, CONNECT)
  private val trace = Routes.createForMethod(routes, TRACE)

  private val routesStr = routes map {_.summary} mkString ", "

  /* Public */

  override def apply(request: Request): Future[Response] = {
    (request.method match {
      case GET => get.handle(request)
      case POST => post.handle(request)
      case PUT => put.handle(request)
      case DELETE => delete.handle(request)
      case OPTIONS => options.handle(request)
      case PATCH => patch.handle(request)
      case HEAD => head.handle(request)
      case CONNECT => connect.handle(request)
      case TRACE => trace.handle(request)
      case _ => badRequest(request.method)
    }).getOrElse {
      notFound(request)
    }
  }

  /* Private */

  private def notFound(request: Request): Future[Response] = {
    debug(request + " not found in " + routesStr)
    Future.value(
      SimpleResponse(
        Status.NotFound,
        request.uri + " route not found"))
  }

  private def badRequest(method: HttpMethod): Option[Future[Response]] = {
    Some(Future.value(
      SimpleResponse(
        Status.BadRequest,
        method.getName + " is not a valid HTTP method")))
  }
}
