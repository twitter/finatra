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
import com.twitter.finagle.http.{Request => FinagleRequest, RequestProxy}
import util.Sorting
import com.google.common.base.Splitter
import scala.collection.JavaConversions._

object Request {
  def apply() = new Request(FinagleRequest("/"))
  def apply(path: String) = new Request(FinagleRequest(path))
}

class Request(val request: FinagleRequest) extends RequestProxy {

  var multiParams:  Map[String, MultipartItem]  = Map.empty
  var routeParams:  Map[String, String]         = Map.empty
  var error:        Option[Throwable]           = None

  def accepts: Seq[ContentType] = {
    val accept = this.headers.get("Accept")

    if (accept != null) {
      var acceptParts = Splitter.on(',').split(accept).toArray

      Sorting.quickSort(acceptParts)(AcceptOrdering)

      val seq = acceptParts.map { xs =>
        val part = Splitter.on(";q=").split(xs).toArray.head

        ContentType(part).getOrElse(new ContentType.All)
      }.toSeq

      seq
    } else {
      Seq.empty[ContentType]
    }
  }
}

object AcceptOrdering extends Ordering[String] {

  def getWeight(str: String): Double = {
    val parts = Splitter.on(';').split(str).toArray

    if (parts.length < 2) {
      1.0
    } else {
      try {
        Splitter.on("q=").split(parts(1)).toArray.last.toFloat
      } catch {
        case e:java.lang.NumberFormatException =>
          1.0
      }
    }
  }

  def compare(a: String, b: String): Int = {
    getWeight(b) compare getWeight(a)
  }
}
