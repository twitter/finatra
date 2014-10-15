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

import com.twitter.finatra.test.FlatSpecHelper
import com.twitter.finagle.http.{Request => FinagleRequest}
import org.apache.commons.io.IOUtils

class MultipartUploadSpec extends FlatSpecHelper {

  class ExampleApp extends Controller {
    post("/groups_file") { request =>
      val groupsParam = request.multiParams.get("groups")
      val typeParam = request.multiParams.get("type")

      render
        .header("X-Content-Type", groupsParam.get.contentType.toString)
        .header("X-Filename", groupsParam.get.filename.toString)
        .header("X-Type-Text", typeParam.get.value)
        .plain("ok").toFuture
    }

  }

  val server = new FinatraServer
  server.register(new ExampleApp)

  "Multi part uploads with text and file fields" should "work" in {

    /***
    This is a serialized request of a multipart upload from Chrome with the form:
      <form enctype="multipart/form-data" action="/groups_file?debug=true" method="POST">
        <label for="groups">Filename:</label>
        <input type="file" name="groups" id="groups"><br>
        <input type="hidden" name="type" value="text"/>
        <input type="submit" name="submit" value="Submit">
      </form>

    The infamous 'dealwithit.gif' was used for the "groups" file
    ***/

    val s = getClass.getResourceAsStream("/upload.bytes")
    val b = IOUtils.toByteArray(s)
    val r = FinagleRequest.decodeBytes(b)
    send(r)

    response.code should equal (200)
    response.getHeader("X-Content-Type") should equal("Some(image/gif)")
    response.getHeader("X-Filename") should equal("Some(dealwithit.gif)")
    response.getHeader("X-Type-Text") should equal("text")
  }

}
