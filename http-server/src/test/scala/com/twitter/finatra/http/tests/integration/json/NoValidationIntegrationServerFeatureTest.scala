package com.twitter.finatra.http.tests.integration.json

import com.google.inject.Module
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.server.FeatureTest

class NoValidationIntegrationServerFeatureTest extends FeatureTest {

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "validation-server"
      override def jacksonModule: Module = new ScalaObjectMapperModule {
        override val validation = false
      }
      override protected def configureHttp(router: HttpRouter): Unit = {
        val validationController = new Controller {
          post("/validate_user") { _: ValidateUserRequest =>
            "You passed!"
          }
        }
        router
          .add(validationController)
      }
    },
    disableTestLogging = true
  )

  test("server should skip validation logic") {
    server.httpPost(
      "/validate_user",
      andExpect = Ok,
      postBody = """
          {
            "user_name" : "@&^",
            "id" : "5656",
            "title": "CEO"
          }
        """"
    )
  }
}
