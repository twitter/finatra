package com.twitter.finatra

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import org.jboss.netty.handler.codec.http.HttpMethod
import scala.collection.mutable.ListBuffer
import scala.collection.Map
import com.twitter.util.Future

class Router(controller: Controller) extends Logging {

  def dispatch(request: FinagleRequest): Option[FinagleResponse] = {
    logger.info("%s %s".format(request.method, request.uri))

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
    orCallback: FinagleRequest => Option[FinagleResponse]
  ): Option[FinagleResponse] = {
    val req = RequestAdapter(request)

    findRouteAndMatch(req, method) match {
      case Some((method, pattern, callback)) =>
        Some(ResponseAdapter(callback(req))).asInstanceOf[Option[FinagleResponse]]
      case None => orCallback(request)
    }
  }

  def extractParams(request: Request, xs: Tuple2[_, _]): Map[String, String] =
    request.routeParams += (xs._1.toString -> xs._2.asInstanceOf[ListBuffer[String]].head.toString)

  def findRouteAndMatch(request: Request, method: HttpMethod):
    Option[(HttpMethod, PathPattern, (Request) => Future[Response])] = {

    var thematch: Option[Map[_,_]] = None

    controller.routes.vector.find( route => route match {
      case (_method, pattern, callback) =>
        thematch = pattern(request.path.split('?').head)
        if(thematch.orNull != null && _method == method) {
          thematch.orNull.foreach(xs => extractParams(request, xs))
          true
        } else {
          false
        }
    })
  }

  def internalDispatch (
    method:   HttpMethod,
    path:     String,
    params:   Map[String, String] = Map(),
    headers:  Map[String, String] = Map()
  ): Future[Response] = {

    val finagleRequest = FinagleRequest(path, params.toList:_*)

    finagleRequest.httpRequest.setMethod(method)

    headers.foreach { header =>
      finagleRequest.httpRequest.setHeader(header._1, header._2)
    }

    val req = new Request(finagleRequest)

    findRouteAndMatch(req, method) match {
      case Some((_method, pattern, callback)) =>
        callback(req)
      case None => controller.render.notFound.toFuture
    }
  }

  def get(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[Response] = internalDispatch(HttpMethod.GET, path, params, headers)

  def post(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[Response] = internalDispatch(HttpMethod.POST, path, params, headers)

  def put(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[Response] = internalDispatch(HttpMethod.PUT, path, params, headers)

  def delete(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[Response] = internalDispatch(HttpMethod.DELETE, path, params, headers)

  def head(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
    Future[Response] = internalDispatch(HttpMethod.HEAD, path, params, headers)

  def patch(path: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()):
  Future[Response] = internalDispatch(HttpMethod.PATCH, path, params, headers)

}
