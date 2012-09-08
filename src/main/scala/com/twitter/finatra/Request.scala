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

import scala.collection.mutable.Map
import com.twitter.finatra_core.FinatraRequest
import org.jboss.netty.handler.codec.http.Cookie

class Request extends FinatraRequest {

  var path: String                            = "/"
  var method: String                          = "GET"
  var body: Array[Byte]                       = Array()
  var params: Map[String, String]             = Map()
  var multiParams: Map[String, MultipartItem] = Map()
  var headers: Map[String, String]            = Map()
  var cookies: Map[String, Cookie]            = Map()

  def finatraPath   = path
  def finatraMethod = method
  def finatraParams = params

  def path(p:String):Request = {
    this.path = p
    this
  }

  def method(m: String):Request = {
    this.method = m
    this
  }

  def body(b: Array[Byte]):Request = {
    this.body = b
    this
  }

  def params(m: Map[String, String]):Request = {
    this.params = this.params ++ m
    this
  }

  def headers(m: Map[String, String]):Request = {
    this.headers = this.headers ++ m
    this
  }

  def cookies(m: Map[String, Cookie]):Request = {
    this.cookies = this.cookies ++ m
    this
  }

}
