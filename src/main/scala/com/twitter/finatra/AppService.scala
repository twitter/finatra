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

import com.twitter.finagle.Service
import com.twitter.util.{Await, Future}
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.app.App

class AppService(controllers: ControllerCollection)
  extends Service[FinagleRequest, FinagleResponse] with App with Logging {

  def render: ResponseBuilder = new ResponseBuilder

  def apply(rawRequest: FinagleRequest): Future[FinagleResponse] = {
    val adaptedRequest  = RequestAdapter(rawRequest)

    def handleError(t:Throwable) = {
      log.error(t, "Internal Server Error")
      adaptedRequest.error = Some(t)
      ResponseAdapter(adaptedRequest, controllers.errorHandler(adaptedRequest))
    }

    try {
      attemptRequest(rawRequest).handle {
        case t:Throwable =>
          Await.result(handleError(t))
      }
    } catch {
      case e: Exception =>
        handleError(e)
    }
  }

  def attemptRequest(rawRequest: FinagleRequest): Future[FinagleResponse] = {
    val adaptedRequest = RequestAdapter(rawRequest)

    controllers.dispatch(rawRequest) match {
      case Some(response) =>
        response.asInstanceOf[Future[FinagleResponse]]
      case None =>
        ResponseAdapter(adaptedRequest, controllers.notFoundHandler(adaptedRequest))
    }
  }

}
