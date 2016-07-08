package com.twitter.finatra.http.tests.integration.messagebody.test

import com.google.common.net.MediaType
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.tests.integration.messagebody.main.GreetingServer
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class GreetingControllerIntegrationTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    new GreetingServer,
    defaultRequestHeaders = Map(),
    flags = Map())

  val requestPath = "/greet?name=Bob"

  "get English greeting" in {
    server.httpGet(
      path = requestPath,
      andExpect = Status.Ok,
      withBody = "Hello Bob")
  }

  "get Spanish greeting" in {
    server.httpGet(
      path = requestPath,
      headers = Map("Accept-Language" -> "es"),
      andExpect = Status.Ok,
      withBody = "Hola Bob")
  }

  "get English json greeting" in {
    server.httpGet(
      path = requestPath,
      accept = MediaType.JSON_UTF_8,
      andExpect = Status.Ok,
      withJsonBody = """{ "greeting" : "Hello Bob" }""")
  }

  "get Spanish json greeting" in {
    server.httpGet(
      path = requestPath,
      accept = MediaType.JSON_UTF_8,
      headers = Map("Accept-Language" -> "es"),
      andExpect = Status.Ok,
      withJsonBody = """{ "greeting" : "Hola Bob" }""")
  }

}
