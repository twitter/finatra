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
import org.jboss.netty.handler.codec.http._
import com.twitter.logging.Logging
import com.twitter.app.App

class Controller(statsReceiver: StatsReceiver = NullStatsReceiver) {

  val routes = new RouteVector[(HttpMethod, PathPattern, Request => Future[Response])]

  var notFoundHandler:  Option[(Request) => Future[Response]]  = None
  var errorHandler:     Option[(Request) => Future[Response]]  = None

  def get(path: String)   (callback: Request => Future[Response]) { addRoute(HttpMethod.GET,    path)(callback) }
  def delete(path: String)(callback: Request => Future[Response]) { addRoute(HttpMethod.DELETE, path)(callback) }
  def post(path: String)  (callback: Request => Future[Response]) { addRoute(HttpMethod.POST,   path)(callback) }
  def put(path: String)   (callback: Request => Future[Response]) { addRoute(HttpMethod.PUT,    path)(callback) }
  def head(path: String)  (callback: Request => Future[Response]) { addRoute(HttpMethod.HEAD,   path)(callback) }
  def patch(path: String) (callback: Request => Future[Response]) { addRoute(HttpMethod.PATCH,  path)(callback) }
  def options(path: String)(callback: Request => Future[Response]){ addRoute(HttpMethod.OPTIONS, path)(callback) }

  def notFound(callback: Request => Future[Response]) {
    notFoundHandler = Option(callback)
  }

  def error(callback: Request => Future[Response]) {
    errorHandler = Option(callback)
  }

  val stats = statsReceiver.scope("Controller")

  def render: Response  = new Response
  def route:  Router    = new Router(this)

  def redirect(location: String, message: String = "", permanent: Boolean = false): Response = {
    val msg = if (message == "")
      "Redirecting to <a href=\"%s\">%s</a>.".format(location, location)
    else
      message

    val code = if (permanent) 301 else 302

    render.plain(msg).status(code).header("Location", location)
  }

  def respondTo(r: Request)(callback: PartialFunction[ContentType, Future[Response]]): Future[Response] = {
    if (!r.routeParams.get("format").isEmpty) {
      val format      = r.routeParams("format")
      val mime        = FileService.getContentType("." + format)
      val contentType = ContentType(mime).getOrElse(new ContentType.All)

      if (callback.isDefinedAt(contentType)) {
        callback(contentType)
      } else {
        throw new UnsupportedMediaType
      }
    } else {
      r.accepts.find { mimeType =>
        callback.isDefinedAt(mimeType)
      } match {
        case Some(contentType) =>
          callback(contentType)
        case None =>
          throw new UnsupportedMediaType
      }
    }
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
