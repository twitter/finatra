package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status.BadRequest
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
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

            get("/users/lookup") {
              request: UserLookupRequest =>
                Map(
                  "ids" -> request.ids,
                  "names" -> request.names,
                  "format" -> request.format,
                  "userFormat" -> request.userFormat,
                  "statusFormat" -> request.statusFormat,
                  "acceptHeader" -> request.acceptHeader,
                  "validationPassesForIds" -> request.validationPassesForIds,
                  "validationPassesForNames" -> request.validationPassesForNames
                )
            }
          })
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
      withJsonBody = """{"errors":["things: Unable to parse"]}"""
    )
  }

  test("/GET UserLookup") {

    val response: Response = server.httpGet(
      "/users/lookup?ids=21345",
      headers = Map("accept" -> "application/vnd.foo+json")
    )

    response.status.code shouldBe 200
    val responseMap = server.mapper.parse[Map[String, String]](response.contentString)
    responseMap("ids") should be("21345")
    responseMap("format") should be(null)
    responseMap("userFormat") should be(null)
    responseMap("statusFormat") should be(null)
    responseMap("validationPassesForIds").toBoolean should be(true)
    responseMap("validationPassesForNames").toBoolean should be(true)
    responseMap("acceptHeader") should be("application/vnd.foo+json")
  }
}
