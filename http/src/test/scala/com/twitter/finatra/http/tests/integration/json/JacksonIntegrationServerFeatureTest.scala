package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.Status.BadRequest
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.server.FeatureTest

class JacksonIntegrationServerFeatureTest extends FeatureTest {

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "jackson-server"

      override protected def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[CommonFilters]
          .exceptionMapper[CaseClassMappingExceptionMapper]
          .add(new Controller {
              post("/personWithThings") { _: PersonWithThingsRequest =>
                "Accepted"
              }
            }
          )
      }
    },
    disableTestLogging = true
  )

  /** Verify users can choose to not "leak" information via the ExceptionMapper */

  test("/POST /personWithThings") {
    server.httpPost(
      "/personWithThings",
      """
          {
            "id" :1,
            "name" : "Bob",
            "age" : 21,
            "things" : {
              "foo" : [
                "IhaveNoKey"
              ]
            }
          }
      """,
      andExpect = BadRequest,
      withJsonBody = """{"errors":["things: Unable to parse"]}""")
  }
}
