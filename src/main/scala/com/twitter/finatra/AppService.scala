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

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.util.CharsetUtil.UTF_8


class AppService extends Service[HttpRequest, HttpResponse]{

  def notFoundResponse = {
    val resp = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
    resp.setContent(copiedBuffer("not found", UTF_8))
    Future.value(resp)
  }

  def apply(rawRequest: HttpRequest) = {
    val request = RequestAdapter(rawRequest)

    FinatraServer.controllers.dispatch(request) match {
      case Some(response) =>
        response.asInstanceOf[Future[HttpResponse]]
      case None =>
        notFoundResponse
    }
  }

}
