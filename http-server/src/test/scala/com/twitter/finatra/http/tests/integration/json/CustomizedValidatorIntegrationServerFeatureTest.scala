package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.TwitterModule
import com.twitter.inject.server.FeatureTest

class CustomizedValidatorIntegrationServerFeatureTest extends FeatureTest {

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "validation-server"
      override def validatorModule: TwitterModule = CustomizedValidatorModule

      override protected def configureHttp(router: HttpRouter): Unit = {
        router
          .add[ValidationController]
          .exceptionMapper[CaseClassMappingExceptionMapper]
      }
    },
    disableTestLogging = true
  )

  test("server should use the user injected Validator") {
    server.httpGet(
      "/validate_things",
      andExpect = Ok,
      withBody = "\nValidation Errors:\t\t" + "names: Whatever you provided is wrong." + "\n\n"
    )
  }

  test("ScalaObjectMapper should use the customized validator") {
    server.httpGet(
      "/parse_things",
      andExpect = Ok,
      withBody = "names: Whatever you provided is wrong."
    )
  }
}
