package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.http.Method._
import com.twitter.finatra.http.AnyMethod
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.inject.Logging
import com.twitter.util.Future

private[http] class RoutingService(
  routes: Seq[Route])
  extends Service[Request, Response]
  with Logging {

  private val get = Routes.createForMethod(routes, Get)
  private val post = Routes.createForMethod(routes, Post)
  private val put = Routes.createForMethod(routes, Put)
  private val delete = Routes.createForMethod(routes, Delete)
  private val options = Routes.createForMethod(routes, Options)
  private val patch = Routes.createForMethod(routes, Patch)
  private val head = Routes.createForMethod(routes, Head)
  private val trace = Routes.createForMethod(routes, Trace)
  private val any = Routes.createForMethod(routes, AnyMethod)

  private val routesStr = routes map {_.summary} mkString ", "

  /* Public */

  override def apply(request: Request): Future[Response] = {
    (request.method match {
      case Get => get.handle(request)
      case Post => post.handle(request)
      case Put => put.handle(request)
      case Delete => delete.handle(request)
      case Options => options.handle(request)
      case Patch => patch.handle(request)
      case Head => head.handle(request)
      case Trace => trace.handle(request)
      case _ => badRequest(request.method)
    }).getOrElse {
      any.handle(request)
        .getOrElse(notFound(request))
    }
  }

  /* Private */

  private def notFound(request: Request): Future[Response] = {
    debug(request + " not found in registered routes: " + routesStr)
    Future.value(
      SimpleResponse(
        Status.NotFound))
  }

  private def badRequest(method: Method): Option[Future[Response]] = {
    Some(Future.value(
      SimpleResponse(
        Status.BadRequest,
        method.toString + " is not a valid HTTP method")))
  }
}