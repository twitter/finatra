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
import com.twitter.finagle.http.{Response => FinagleResponse, Request => FinagleRequest}
import com.twitter.finatra.FinatraServer
import org.jboss.netty.handler.codec.http.HttpMethod

class MockResponse(val originalResponse: FinagleResponse) {

  def status                  = originalResponse.getStatus
  def code                    = originalResponse.getStatus.getCode
  def body                    = originalResponse.getContent.toString(UTF_8)
  def getHeader(name: String) = originalResponse.headers.get(name)
  def getHeaders              = originalResponse.headerMap

}

trait SpecHelper {

  def response  = new MockResponse(Await.result(lastResponse))
  var lastResponse: Future[FinagleResponse] = null

  def server: FinatraServer

  def get(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    executeRequest(HttpMethod.GET,path,params,headers)
  }

  def post(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map(), body:AnyRef=null) {
    executeRequest(HttpMethod.POST,path,params,headers,body)
  }

  def put(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map(), body:AnyRef=null) {
    executeRequest(HttpMethod.PUT,path,params,headers,body)
  }

  def delete(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    executeRequest(HttpMethod.DELETE,path,params,headers)
  }

  def head(path:String,params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    executeRequest(HttpMethod.HEAD,path,params,headers)
  }

  def patch(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    executeRequest(HttpMethod.PATCH,path,params,headers)
  }

  def options(path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map(), body:AnyRef=null) {
    executeRequest(HttpMethod.OPTIONS,path,params,headers,body)
  }

  def send(request: FinagleRequest) {
    executeRequest(request)
  }

  private def executeRequest(
    method: HttpMethod,
    path: String,
    params: Map[String, String] = Map(),
    headers: Map[String,String] = Map(),
    body: AnyRef = null
    ) {
    val app = MockApp(server)
    val result: MockResult = app.execute(method = method, path = path, params = params, headers = headers, body = body)
    lastResponse = result.response
  }

  private def executeRequest(request: FinagleRequest) {
    val app = MockApp(server)
    val result: MockResult = app.execute(request)
    lastResponse = result.response
  }

}

