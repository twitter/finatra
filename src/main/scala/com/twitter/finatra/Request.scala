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
import com.twitter.finagle.http.{Request => FinagleRequest, RequestProxy}

class Request(rawRequest: FinagleRequest) extends RequestProxy with FinatraRequest {

  var body: Array[Byte]                       = Array()
  var multiParams: Map[String, MultipartItem] = Map.empty
  var routeParams:Map[String, String] = Map.empty

  var request = rawRequest

  def finatraPath   = path
  def finatraMethod = method.toString
  def finatraParams = routeParams


}
