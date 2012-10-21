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
import scala.Tuple2
import scala.Some
import collection.mutable.ListBuffer

class Controller(statsReceiver: StatsReceiver = NullStatsReceiver) extends Logging {


  /*
   * Adds the route to the routing table
   */

  val responseConverter = new FinatraResponseConverter

  val routes = new RouteVector[(String, PathPattern, Request => Future[Response])]

  def get(path: String)   (callback: Request => Future[Response]) { addRoute("GET",    path)(callback) }
  def delete(path: String)(callback: Request => Future[Response]) { addRoute("DELETE", path)(callback) }
  def post(path: String)  (callback: Request => Future[Response]) { addRoute("POST",   path)(callback) }
  def put(path: String)   (callback: Request => Future[Response]) { addRoute("PUT",    path)(callback) }
  def head(path: String)  (callback: Request => Future[Response]) { addRoute("HEAD",   path)(callback) }
  def patch(path: String) (callback: Request => Future[Response]) { addRoute("PATCH",  path)(callback) }


  def dispatch(request: Request): Option[FinagleResponse] = {
    dispatchRouteOrCallback(request, request.method.toString, (request) => {
      // fallback to GET for 404'ed GET requests (curl -I support)
      if (request.method.toString == "HEAD") {
        dispatchRouteOrCallback(request, "GET", (request) => None)
      } else {
        return None
      }
    })
  }

  def dispatchRouteOrCallback(request: Request, method: String,
                              orCallback: Request => Option[FinagleResponse]): Option[FinagleResponse] = {

    findRouteAndMatch(request, method) match {
      case Some((method, pattern, callback)) => Some(responseConverter(callback(request))).asInstanceOf[Option[FinagleResponse]]
      case None => orCallback(request)
    }
  }

  def extractParams(request: Request, xs: Tuple2[_, _]) = {
    request.routeParams += (xs._1.toString -> xs._2.asInstanceOf[ListBuffer[String]].head.toString)
  }

  def findRouteAndMatch(request: Request, method: String) = {
    var thematch:Option[Map[_,_]] = None
    println("looking for path:" + request.path)
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

//  def addRoute(method: String, path: String)(callback: Request => Future[Response]) {
//    addRoute(method, path) { request =>
//      stats.timeFuture("%s/Root/%s".format(method, path.stripPrefix("/"))) {
//        callback(request)
//      }
//    }
//  }
  def addRoute(method: String, path: String)(callback: Request => Future[Response]) {
    val regex = SinatraPathPatternParser(path)
    routes.add((method, regex, callback))
  }
}
