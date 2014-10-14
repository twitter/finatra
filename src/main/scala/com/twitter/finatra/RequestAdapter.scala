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

import com.twitter.finagle.http.{Request => FinagleRequest}
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder

/**
* Adapts a FinagleRquest to a FinatraRequest
*/
object RequestAdapter {

  def apply(rawRequest: FinagleRequest): Request = {
    val request = new Request(rawRequest)

    request.getContent.markReaderIndex

    if (request.method == HttpMethod.POST && HttpPostRequestDecoder.isMultipart(request)) {
      request.multiParams = MultipartParsing(request)
    }

    request.getContent.resetReaderIndex()

    request
  }
}
