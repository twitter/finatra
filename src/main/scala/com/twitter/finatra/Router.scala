package com.twitter.finatra

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import org.jboss.netty.handler.codec.http.{QueryStringDecoder, HttpMethod}
import scala.collection.mutable.ListBuffer
import scala.collection.Map
import com.twitter.util.Future
import com.twitter.app.App
import org.jboss.netty.util.CharsetUtil

class Router(controller: Controller) {

  def dispatch(request: FinagleRequest): Option[Future[FinagleResponse]] = {
    dispatchRouteOrCallback(request, request.method, (request) => {
      // fallback to GET for 404'ed GET requests (curl -I support)
      if (request.method == HttpMethod.HEAD) {
        dispatchRouteOrCallback(request, HttpMethod.GET, (request) => None)
      } else {
        return None
      }
    })
  }

  def dispatchRouteOrCallback(
    request:    FinagleRequest,
    method:     HttpMethod,
    orCallback: FinagleRequest => Option[Future[FinagleResponse]]
  ): Option[Future[FinagleResponse]] = {
    val req = RequestAdapter(request)

    findRouteAndMatch(req, method) match {
      case Some((method, definition, pattern, callback)) =>
        Some(ResponseAdapter(req, callback(req)))
      case None => orCallback(request)
    }
  }

  def extractParams(request: Request, xs: Tuple2[_, _]): Map[String, String] = {
    val decodedKey = QueryStringDecoder.decodeComponent(xs._1.toString, CharsetUtil.UTF_8)
    val decodedValue = QueryStringDecoder.decodeComponent(xs._2.asInstanceOf[ListBuffer[String]].head.toString)
    request.routeParams += (decodedKey -> decodedValue)
  }

  def findRouteAndMatch(request: Request, method: HttpMethod):
    Option[(HttpMethod, String, PathPattern, (Request) => Future[ResponseBuilder])] = {

    controller.routes.vector.find{
      case (_method, definition, pattern, callback) =>
        pattern(request.path.split('?').head) match {
          case Some(thematch) if _method == method => thematch.foreach((xs: Tuple2[_, _]) => extractParams(request, xs)); true
          case _  => false
        }
    }
  }

  def internalDispatch (
    method:   HttpMethod,
    path:     String,
    params:   Map[String, String] = Map(),
    headers:  Map[String, String] = Map()
  ): Future[ResponseBuilder] = {

    val finagleRequest = FinagleRequest(path, params.toList:_*)

    finagleRequest.httpRequest.setMethod(method)

    headers.foreach { header =>
      finagleRequest.httpRequest.headers.set(header._1, header._2)
    }

    val req = new Request(finagleRequest)

    findRouteAndMatch(req, method) match {
      case Some((_method, definition, pattern, callback)) =>
        callback(req)
      case None => controller.render.notFound.toFuture
    }
  }

  def get(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.GET, path, params, headers)

  def post(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.POST, path, params, headers)

  def put(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.PUT, path, params, headers)

  def delete(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.DELETE, path, params, headers)

  def head(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.HEAD, path, params, headers)

  def patch(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.PATCH, path, params, headers)

  def options(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[ResponseBuilder] = internalDispatch(HttpMethod.OPTIONS, path, params, headers)

}
