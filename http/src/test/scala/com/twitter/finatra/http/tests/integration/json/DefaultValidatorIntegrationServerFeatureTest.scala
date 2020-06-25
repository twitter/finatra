package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest

class DefaultValidatorIntegrationServerFeatureTest extends FeatureTest {
  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "default-validator-server"

      override protected def configureHttp(router: HttpRouter): Unit = {
        router.add[ValidationController]
      }
    },
    disableTestLogging = true
  )

  test("server should inject the default Validator") {
    server.httpGet(
      "/validate_things",
      andExpect = Ok,
      withBody = "\nValidation Errors:\t\t" + "size [0] is not between 1 and 2" + "\n\n"
    )
  }

  test("ScalaObjectMapper should use the default validator") {
    server.httpGet(
      "/parse_things",
      andExpect = Ok,
      withBody = "names: size [0] is not between 1 and 2"
    )
  }
}
