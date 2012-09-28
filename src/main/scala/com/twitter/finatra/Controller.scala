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
import com.twitter.finatra_core.AbstractFinatraController
import com.twitter.util.Future
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}

class Controller(statsReceiver: StatsReceiver = NullStatsReceiver)
  extends AbstractFinatraController[Request, Future[Response], Future[FinagleResponse]]
  with Logging {

  val stats = statsReceiver.scope("Controller")

  override val responseConverter = new FinatraResponseConverter

  def render = new Response

  def redirect(location: String, message: String = "moved") = {
    render.plain(message).status(301).header("Location", location)
  }

  override def addRoute(method: String, path: String)(callback: Request => Future[Response]) {
    super.addRoute(method, path) { request =>
      stats.timeFuture("%s/Root/%s".format(method, path.stripPrefix("/"))) {
        callback(request)
      }
    }
  }
}
