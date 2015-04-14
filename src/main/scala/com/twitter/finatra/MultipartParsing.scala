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
import scala.collection.JavaConversions._
import com.twitter.finagle.http.{Request => FinagleRequest}
import org.jboss.netty.handler.codec.http.multipart.{MixedAttribute, MixedFileUpload, HttpPostRequestDecoder}

object MultipartParsing {

  def apply(request: FinagleRequest) = {
    var multiParams = Map[String, MultipartItem]()

    val dec = new HttpPostRequestDecoder(request)
      if (dec.isMultipart) {
        dec.getBodyHttpDatas.foreach {

          case data: MixedFileUpload =>
            val mpi = new MultipartItem(data.get, data.getName, Some(data.getContentType), Some(data.getFilename))
            multiParams += (data.getName -> mpi)

          case data: MixedAttribute =>
            val mpi = new MultipartItem(data.get, data.getName, None, None)
            multiParams += (data.getName -> mpi)

        }
    }

    dec.cleanFiles()

    multiParams
  }

}
