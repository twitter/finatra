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

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil.UTF_8

import com.twitter.finatra.ResponseBuilder
import com.twitter.finatra.View
import com.twitter.finagle.http.Status

class MockView(val title:String) extends View {
  val template = "mock.mustache"
}

class ResponseBuilderSpec extends ShouldSpec {
  def resp = new ResponseBuilder
  def view = new MockView("howdy view")
  def buffer = ChannelBuffers.wrappedBuffer("buffer".getBytes(UTF_8))

  ".status(201)" should "return a 201 response" in {
    val built = resp.status(201).build

    built.statusCode should equal (201)
    built.headerMap.get("Content-Length").get.toInt should equal (0)
  }

  ".plain()" should "return a 200 plain response" in {
    val response = resp.plain("howdy")
    val built    = response.build

    built.statusCode should equal (200)
    built.contentString should equal ("howdy")
    built.contentType should equal (Some("text/plain"))
    built.headerMap.get("Content-Length").get.toInt should equal (5)
  }

  ".nothing()" should "return a 200 empty response" in {
    val response = resp.nothing
    val built    = response.build

    built.statusCode should equal (200)
    built.contentString should equal ("")
    built.contentType should equal (Some("text/plain"))
    built.headerMap.get("Content-Length").get.toInt should equal (0)
  }

  ".html()" should "return a 200 html response" in {
    val response = resp.html("<h1>howdy</h1>")
    val built = response.build

    built.statusCode should equal (200)
    built.contentString should equal ("<h1>howdy</h1>")
    built.contentType should equal (Some("text/html"))
    built.headerMap.get("Content-Length").get.toInt should equal (14)
  }

  ".json()" should "return a 200 json response" in {
    val response = resp.json(Map("foo" -> "bar"))
    val built    = response.build
    val body     = built.getContent.toString(UTF_8)

    built.statusCode should equal (200)
    body should equal ("""{"foo":"bar"}""")
    built.contentType should equal (Some("application/json"))
    built.headerMap.get("Content-Length").get.toInt should equal (13)
  }

  ".json()" should "return a 200 json response with correct Content-Length for unicode strings" in {
    val response = resp.json(Map("foo" -> "⛄"))
    val built    = response.build
    val body     = built.getContent.toString(UTF_8)

    built.statusCode should equal (200)
    body should equal ("""{"foo":"⛄"}""")
    built.contentType should equal (Some("application/json"))
    built.headerMap.get("Content-Length").get.toInt should equal (13)
  }

  ".view()" should "return a 200 view response" in {
    val response = resp.view(view)
    val built    = response.build
    val body     = built.getContent.toString(UTF_8)

    built.statusCode should equal (200)
    body should include ("howdy view")
    built.headerMap.get("Content-Length").get.toInt should equal (11) // 10 character from the title, plus one for the newline in the template
  }

  ".static()" should "return a 200 static file" in {
    val response = resp.static("dealwithit.gif")
    val built = response.build

    built.statusCode should equal (200)
    built.contentType should equal (Some("image/gif"))
    built.headerMap.get("Content-Length").get.toInt should equal (422488)
  }

  ".buffer()" should "return a 200 buffer response" in {
    val response = resp.buffer(buffer)
    val built    = response.build
    val body     = built.getContent.toString(UTF_8)

    built.statusCode should equal (200)
    body should include ("buffer")
    built.headerMap.get("Content-Length").get.toInt should equal (6)
  }

}


class CommonStatusesSpec extends ShouldSpec {
  def resp = new ResponseBuilder

  Seq(

    (".ok",                  resp.ok,                  Status.Ok),
    (".created",             resp.created,             Status.Created),
    (".accepted",            resp.accepted,            Status.Accepted),
    (".movedPermanently",    resp.movedPermanently,    Status.MovedPermanently),
    (".found",               resp.found,               Status.Found),
    (".notModified",         resp.notModified,         Status.NotModified),
    (".temporaryRedirect",   resp.temporaryRedirect,   Status.TemporaryRedirect),
    (".badRequest",          resp.badRequest,          Status.BadRequest),
    (".unauthorized",        resp.unauthorized,        Status.Unauthorized),
    (".forbidden",           resp.forbidden,           Status.Forbidden),
    (".notFound",            resp.notFound,            Status.NotFound),
    (".gone",                resp.gone,                Status.Gone),
    (".internalServerError", resp.internalServerError, Status.InternalServerError),
    (".notImplemented",      resp.notImplemented,      Status.NotImplemented),
    (".serviceUnavailable",  resp.serviceUnavailable,  Status.ServiceUnavailable)

  ).foreach { case (actionName, actualResponseBuilder, expectedStatus) =>
    val testMessage = "return a %s response" format expectedStatus
    actionName should testMessage in {
      val built = actualResponseBuilder.build
      built.status should equal(expectedStatus)
    }
  }
}
