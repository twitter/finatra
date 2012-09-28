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
package com.twitter.finatra.test

import com.twitter.finatra._
import com.twitter.finatra_core.AbstractFinatraSpec
import com.twitter.util.Future
import scala.collection.mutable.Map
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}

class MockResponse(val originalResponse: FinagleResponse) {

  def status                  = originalResponse.getStatus
  def code                    = originalResponse.getStatus.getCode
  def body                    = originalResponse.getContent.toString(UTF_8)
  def getHeader(name: String) = originalResponse.getHeader(name)
  def getHeaders              = originalResponse.getHeaders

}

abstract class SpecHelper extends AbstractFinatraSpec[Request, Future[Response], Future[FinagleResponse]] {

  def response  = new MockResponse(lastResponse.get)
  def request   = new Request

  var lastResponse:Future[FinagleResponse] = null

  def buildRequest(method:String, path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    val req = request
    req.method(method)
    req.path(path)
    req.params(params)
    req.headers(headers)
    lastResponse = app.dispatch(req).asInstanceOf[Option[Future[FinagleResponse]]].get
  }
}
