package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Fields, Method, Request, Response, Status}
import com.twitter.finagle.http.Method._
import com.twitter.finatra.http.exceptions.MethodNotAllowedException
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.inject.Logging
import com.twitter.util.Future

private[http] class RoutingService(routes: Seq[Route])
    extends Service[Request, Response]
    with Logging {

  private val routesToMatch = Routes.createRoutes(routes)
  private val routesStr = routes map { _.summary } mkString ", "

  /* Public */

  override def apply(request: Request): Future[Response] = {
    route(request, bypassFilters = false)
  }

  private[finatra] def route(request: Request, bypassFilters: Boolean): Future[Response] = {
    request.method match {
      case Connect | Get | Post | Put | Delete | Options | Patch | Head | Trace =>
        try {
          routesToMatch
            .handle(request, bypassFilters)
            .getOrElse(notFound(request))
        } catch {
          case _: MethodNotAllowedException =>
            methodNotAllowed(request.method, request.path)
        }
      case _ => badRequest(request.method)
    }
  }

  /* Private */

  private def notFound(request: Request): Future[Response] = {
    debug(request + " not found in registered routes: " + routesStr)
    Future.value(SimpleResponse(Status.NotFound))
  }

  private def badRequest(method: Method): Future[Response] = {
    Future
      .value(SimpleResponse(Status.BadRequest, method.toString + " is not a valid HTTP method"))
  }

  private def methodNotAllowed(method: Method, path: String): Future[Response] = {
    val methodsAllowed: String = "CONNECT, GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD, TRACE"
    val response = SimpleResponse(
      Status.MethodNotAllowed,
      s"${method.toString} is not allowed on path $path")
    response.headerMap.set(Fields.Allow, methodsAllowed)
    Future.value(response)
  }
}
