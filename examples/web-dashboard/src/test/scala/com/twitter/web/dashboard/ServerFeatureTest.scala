package com.twitter.web.dashboard

import com.google.common.net.MediaType
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ServerFeatureTest extends FeatureTest {

  override val server =
    new EmbeddedHttpServer(
      twitterServer = new Server,
      disableTestLogging = true)

  test("/ping") {
    val response = server.httpGet(
      "/ping",
      andExpect = Status.Ok,
      withBody = "pong")

    assert(response.contentType.isDefined)
    assert(response.contentType.get == MediaType.PLAIN_TEXT_UTF_8.toString)
  }

  test("/user") {
    val response = server.httpGet(
      "/user?first=Jane&last=Doe",
      andExpect = Status.Ok,
      withBody =
        """First Name: Jane
          |<br />
          |Last Name: Doe""".stripMargin)

    assert(response.contentType.isDefined)
    assert(response.contentType.get == MediaType.HTML_UTF_8.toString)
  }

  test("/other") {
    val response = server.httpGet(
      "/other",
      andExpect = Status.Ok)

    assert(response.contentType.isDefined)
    assert(response.contentType.get == MediaType.HTML_UTF_8.toString)
  }

  test("/document.xml") {
    val response = server.httpGet(
      "/document.xml",
      andExpect = Status.Ok)

    assert(response.contentType.isDefined)
    assert(response.contentType.get == MediaType.XML_UTF_8.toString)
  }
}
