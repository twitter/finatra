package com.twitter.finatra.http.tests.integration.messagebody.test

import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.messagebody.main.GreetingServer
import com.twitter.inject.server.FeatureTest

class GreetingControllerIntegrationTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new GreetingServer)

  val requestPath = "/greet?name=Bob"

  test("get English greeting") {
    server.httpGet(path = requestPath, andExpect = Status.Ok, withBody = "Hello Bob")
  }

  test("get Spanish greeting") {
    server.httpGet(
      path = requestPath,
      headers = Map("Accept-Language" -> "es"),
      andExpect = Status.Ok,
      withBody = "Hola Bob"
    )
  }

  test("get English json greeting") {
    server.httpGet(
      path = requestPath,
      accept = MediaType.JsonUtf8,
      andExpect = Status.Ok,
      withJsonBody = """{ "greeting" : "Hello Bob" }"""
    )
  }

  test("get Spanish json greeting") {
    server.httpGet(
      path = requestPath,
      accept = MediaType.JsonUtf8,
      headers = Map("Accept-Language" -> "es"),
      andExpect = Status.Ok,
      withJsonBody = """{ "greeting" : "Hola Bob" }"""
    )
  }
}
