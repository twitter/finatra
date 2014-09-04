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

import com.twitter.finatra.test.ShouldSpec
import com.twitter.util.Await
import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finagle.http.service.NullService


class FileServiceSpec extends ShouldSpec {
  val fileService = new FileService

  // We assert the content, rather than just 200, since FileService always defers to AppService to render the 404.
  "looking up static files" should "return gif content" in {
    val r = FinagleRequest("/dealwithit.gif")
    val response = fileService(r, NullService)
    Await.result(response).getContent().array().length should not equal 0
  }

  "looking up static files with params" should "return gif content" in {
    val r = FinagleRequest("/dealwithit.gif", "foo" -> "bar")
    val response = fileService(r, NullService)
    Await.result(response).getContent().array().length should not equal 0
  }

  "looking up static files" should "set Content-Length" in {
    val r = FinagleRequest("/dealwithit.gif", "foo" -> "bar")
    val response = fileService(r, NullService)
    Await.result(response).contentLength should equal (Some(422488L))
  }
}
