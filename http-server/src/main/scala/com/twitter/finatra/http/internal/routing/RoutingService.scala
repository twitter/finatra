package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.Fields
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.exceptions.RouteParamExtractionException
import com.twitter.finatra.http.exceptions.UnsupportedMethodException
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.util.Future
import com.twitter.util.logging.Logger

private object RoutingService {
  val AllowedMethods: Seq[Method] =
    Seq(Connect, Get, Post, Put, Delete, Options, Patch, Head, Trace)

  val AllowedMethodsStr: String =
    AllowedMethods.map(_.name.toUpperCase).mkString(", ")

  val logger: Logger = Logger(RoutingService.getClass)
}

private[http] class RoutingService(routes: Seq[Route]) extends Service[Request, Response] {

  import RoutingService._

  private[this] val routesToMatch = Routes.createRoutes(routes)
  private[this] val routesStr = routes.map(_.summary).mkString(", ")

  /* Public */

  override def apply(request: Request): Future[Response] = {
    route(request, bypassFilters = false)
  }

  /* Private */

  // optimized
  private[finatra] def route(request: Request, bypassFilters: Boolean): Future[Response] = {
    request.method match {
      case Connect | Get | Post | Put | Delete | Options | Patch | Head | Trace =>
        try {
          routesToMatch
            .handle(request, bypassFilters)
            .getOrElse(notFound(request))
        } catch {
          case _: UnsupportedMethodException =>
            methodNotAllowed(request.method, request.path)
          case e: RouteParamExtractionException =>
            badRequest(e.getMessage)
        }
      case _ =>
        badRequest(request.method.toString + " is not a valid HTTP method")
    }
  }

  private[this] def notFound(request: Request): Future[Response] = {
    logger.debug(request + " not found in registered routes: " + routesStr)
    Future.value(SimpleResponse(Status.NotFound))
  }

  private[this] def badRequest(message: String): Future[Response] = {
    Future.value(SimpleResponse(Status.BadRequest, message))
  }

  private[this] def methodNotAllowed(method: Method, path: String): Future[Response] = {
    val response =
      SimpleResponse(
        Status.MethodNotAllowed,
        s"${method.toString} is not allowed on path $path"
      )
    response.headerMap.set(Fields.Allow, AllowedMethodsStr)
    Future.value(response)
  }
}
