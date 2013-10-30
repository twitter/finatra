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

import com.twitter.util.{Await, Future}
import scala.collection.Map
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finatra.{AppService, ControllerCollection, FinatraServer}
import org.jboss.netty.handler.codec.http.HttpMethod

class MockResponse(val originalResponse: FinagleResponse) {

  def status                  = originalResponse.getStatus
  def code                    = originalResponse.getStatus.getCode
  def body                    = originalResponse.getContent.toString(UTF_8)
  def getHeader(name: String) = originalResponse.getHeader(name)
  def getHeaders              = originalResponse.getHeaders

}

trait SpecHelper {

  def response  = new MockResponse(Await.result(lastResponse))
  var lastResponse:Future[FinagleResponse] = null

  def buildRequest(method: HttpMethod, path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {

    val request = FinagleRequest(path, params.toList:_*)
    request.httpRequest.setMethod(method)
    headers.foreach { header =>
      request.httpRequest.setHeader(header._1, header._2)
    }

    val appService = new AppService(server.controllers)
    val service = server.allFilters(appService)

    lastResponse = service(request)
 }

  def server:FinatraServer

  def get(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.GET,path,params,headers)
  }

  def post(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.POST,path,params,headers)
  }

  def put(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.PUT,path,params,headers)
  }

  def delete(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.DELETE,path,params,headers)
  }

  def head(path:String,params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.HEAD,path,params,headers)
  }

  def patch(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.PATCH,path,params,headers)
  }

  def options(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    buildRequest(HttpMethod.OPTIONS,path,params,headers)
  }

}

