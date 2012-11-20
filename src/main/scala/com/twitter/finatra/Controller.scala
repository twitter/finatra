/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.finatra

import com.twitter.finagle.stats.{StatsReceiver, NullStatsReceiver}
import com.twitter.util.Future
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import org.jboss.netty.handler.codec.http._
import collection.mutable.ListBuffer

class Controller(statsReceiver: StatsReceiver = NullStatsReceiver) extends Logging {

  val routes = new RouteVector[(HttpMethod, PathPattern, Request => Future[Response])]

  var notFoundHandler: Option[(Request) => Future[Response]]  = None
  var errorHandler: Option[(Request) => Future[Response]]     = None

  def get(path: String)   (callback: Request => Future[Response]) { addRoute(HttpMethod.GET,    path)(callback) }
  def delete(path: String)(callback: Request => Future[Response]) { addRoute(HttpMethod.DELETE, path)(callback) }
  def post(path: String)  (callback: Request => Future[Response]) { addRoute(HttpMethod.POST,   path)(callback) }
  def put(path: String)   (callback: Request => Future[Response]) { addRoute(HttpMethod.PUT,    path)(callback) }
  def head(path: String)  (callback: Request => Future[Response]) { addRoute(HttpMethod.HEAD,   path)(callback) }
  def patch(path: String) (callback: Request => Future[Response]) { addRoute(HttpMethod.PATCH,  path)(callback) }

  def notFound(callback: Request => Future[Response]) {
    notFoundHandler = Option(callback)
  }

  def error(callback: Request => Future[Response]) {
    errorHandler = Option(callback)
  }

  def dispatch(request: FinagleRequest): Option[FinagleResponse] = {
    logger.info("%s %s", request.method, request.uri)
    dispatchRouteOrCallback(request, request.method, (request) => {
      // fallback to GET for 404'ed GET requests (curl -I support)
      if (request.method == HttpMethod.HEAD) {
        dispatchRouteOrCallback(request, HttpMethod.GET, (request) => None)
      } else {
        return None
      }
    })
  }

  def dispatchRouteOrCallback(request: FinagleRequest, method: HttpMethod,
                              orCallback: FinagleRequest => Option[FinagleResponse]): Option[FinagleResponse] = {
    val req = RequestAdapter(request)
    findRouteAndMatch(req, method) match {
      case Some((method, pattern, callback)) =>
        Some(ResponseAdapter(callback(req))).asInstanceOf[Option[FinagleResponse]]
      case None => orCallback(request)
    }
  }

  def extractParams(request: Request, xs: Tuple2[_, _]) = {
    request.routeParams += (xs._1.toString -> xs._2.asInstanceOf[ListBuffer[String]].head.toString)
  }

  def findRouteAndMatch(request: Request, method: HttpMethod) = {
    var thematch:Option[Map[_,_]] = None

    routes.vector.find( route => route match {
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

  val stats = statsReceiver.scope("Controller")

  def render = new Response

  def redirect(location: String, message: String = "moved") = {
    render.plain(message).status(301).header("Location", location)
  }

  def addRoute(method: HttpMethod, path: String)(callback: Request => Future[Response]) {
    val regex = SinatraPathPatternParser(path)
    routes.add((method, regex, (r) => {
      stats.timeFuture("%s/Root/%s".format(method.toString, path.stripPrefix("/"))) {
        callback(r)
      }
    }))
  }
}
