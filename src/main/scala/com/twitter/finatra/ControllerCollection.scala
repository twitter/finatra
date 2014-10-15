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

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.util.Future

class ControllerCollection {
  var controllers: Seq[Controller] = Seq.empty

  var notFoundHandler = { request:Request =>
    render.status(404).plain("Not Found").toFuture
  }

  var errorHandler = { request:Request =>
    request.error match {
      case Some(e:com.twitter.finatra.UnsupportedMediaType) =>
        render.status(415).plain("No handler for this media type found").toFuture
      case _ =>
        render.status(500).plain("Something went wrong!").toFuture
    }

  }

  def render: ResponseBuilder = new ResponseBuilder

  def dispatch(request: FinagleRequest): Option[Future[FinagleResponse]] = {
    var response: Option[Future[FinagleResponse]] = None

    controllers.find { ctrl =>
      ctrl.route.dispatch(request) match {
        case Some(callbackResponse) =>
          response = Some(callbackResponse)
          true
        case None =>
          false
      }
    }

    response
  }

  def add(controller: Controller) {
    notFoundHandler = controller.notFoundHandler.getOrElse(notFoundHandler)
    errorHandler    = controller.errorHandler.getOrElse(errorHandler)
    controllers     = controllers ++ Seq(controller)
  }

}
