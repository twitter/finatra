package com.twitter.finatra.http.tests.integration.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.inject.Module
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status.{BadRequest, Ok}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.server.FeatureTest
import com.twitter.util.jackson.ScalaObjectMapper
import scala.collection.JavaConverters._

class JacksonIntegrationServerFeatureTest extends FeatureTest {

  object MixInAnnotationsModule extends SimpleModule {
    setMixInAnnotation(classOf[NotMyType], classOf[NotMyTypeMixIn])
    setMixInAnnotation(classOf[Point], classOf[PointMixin])
  }

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "jackson-server"

      override def jacksonModule: Module = new ScalaObjectMapperModule {
        override val additionalJacksonModules: Seq[com.fasterxml.jackson.databind.Module] =
          Seq(MixInAnnotationsModule)
      }

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

            post("/foo/bar") { request: GenericWithRequest[Int] =>
              Map(
                "data" -> request.data
              )
            }

            get("/with/boolean/:id") { request: WithBooleanRequest =>
              Map("id" -> request.id, "complete_only" -> request.completeOnly)
            }

            get("/queryproperty") { request: CaseClassWithQueryParamJsonPropertyField =>
              request.query
            }

            get("/long/deserializer") { request: CaseClassWithLongAndDeserializerRequest =>
              request.long
            }

            get("/bigdecimal") { request: CaseClassWithCustomDecimalFormatRequest =>
              Map("required" -> request.required, "optional" -> request.optional)
            }

            get("/time") { request: TimeWithFormatRequest =>
              Map("time" -> request.when)
            }

            get("/longparametername") { request: CaseClassWithLongParamNameRequest =>
              request.thisShouldUseMapperNamingStrategy
            }

            get("/notmytype") { request: NotMyType =>
              Map(
                "id" -> request.id,
                "name" -> request.name,
                "description" -> request.description
              )
            }

            get("/thepoint") { request: PointRequest =>
              Map("point" -> request.point)
            }

            get("/complexquery") { request: CaseClassComplexQueryParamRequest =>
              // should be able to parse the string into a JsonNode
              val jsonNode =
                injector.instance[ScalaObjectMapper].parse[JsonNode](request.shouldBeAString)
              jsonNode
                .fieldNames().asScala.map { name =>
                  name -> jsonNode.get(name)
                }.toMap
            }

            get("/shapeless") { request: ShapelessRequest =>
              val fields: Map[String, Any] = request.shape match {
                case r: Rectangle =>
                  Map(
                    "type" -> r.getClass.getSimpleName.toLowerCase(),
                    "width" -> r.width,
                    "height" -> r.height)
                case c: Circle =>
                  Map("type" -> c.getClass.getSimpleName.toLowerCase(), "radius" -> c.radius)
              }
              Map("shape" -> fields)
            }

            post("/shapes") { request: ShapesRequest =>
              Map("shapes" -> request.shapes)
            }

            post("/validate_user") { _: ValidateUserRequest =>
              "You passed!"
            }

            get("/validate_with_http_annotations/:user_name") { request: UserRequestWithParam =>
              s"The user has valid id ${request.userId} and userName ${request.userName}"
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

  /** Test generically typed case class */
  test("/POST /foo/bar") {
    server.httpPost(
      "/foo/bar",
      """
        |{
        |  "data": 42
        |}
      """.stripMargin,
      andExpect = Ok,
      withJsonBody = """{"data": 42}"""
    )
  }

  /**
   * Test that extended booleans (1/0, T/F, t/f) are matched as Booleans with
   * [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]]
   */
  test("/POST /with/boolean") {
    server.httpGet(
      "/with/boolean/12345?complete_only=1",
      andExpect = Ok,
      withJsonBody = """{"id": 12345, "complete_only": true}"""
    )

    server.httpGet(
      "/with/boolean/12345?complete_only=0",
      andExpect = Ok,
      withJsonBody = """{"id": 12345, "complete_only": false}"""
    )

    server.httpGet(
      "/with/boolean/12345?complete_only=T",
      andExpect = Ok,
      withJsonBody = """{"id": 12345, "complete_only": true}"""
    )

    server.httpGet(
      "/with/boolean/12345?complete_only=F",
      andExpect = Ok,
      withJsonBody = """{"id": 12345, "complete_only": false}"""
    )

    server.httpGet(
      "/with/boolean/12345?complete_only=t",
      andExpect = Ok,
      withJsonBody = """{"id": 12345, "complete_only": true}"""
    )

    server.httpGet(
      "/with/boolean/12345?complete_only=f",
      andExpect = Ok,
      withJsonBody = """{"id": 12345, "complete_only": false}"""
    )
  }

  /** Test marshalling into an object with inherited annotations */
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

  /**
   * Test [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]] will ignore
   * extraneous [[com.fasterxml.jackson.annotation.JsonProperty]] annotation (not fail).
   */
  test("/GET ambiguous directive with @QueryParam and @JsonProperty") {
    server.httpGet(
      path = "/queryproperty?q=apples",
      andExpect = Ok,
      withBody = "apples"
    )
  }

  /**
   * Test that [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]] will
   * convert type with [[com.fasterxml.jackson.databind.annotation.JsonDeserialize#contentAs]]
   */
  test("/GET /long/deserializer") {
    server.httpGet(
      path = "/long/deserializer?long=42",
      andExpect = Ok,
      withBody = "42"
    )
  }

  /**
   * Test that the [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]] will
   * look up a specified deserializer with [[com.fasterxml.jackson.databind.annotation.JsonDeserialize#using]]
   */
  test("/GET /bigdecimal") {
    server.httpGet(
      path = "/bigdecimal?required=23.1201&optional=3.145926",
      andExpect = Ok,
      withJsonBody = """{"required": 23.12, "optional": 3.15}"""
    )
  }

  /**
   * Test that [[com.fasterxml.jackson.annotation.JsonFormat]] annotations works on values
   * converted by [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]]
   */
  test("/GET /time") {
    server.httpGet(
      path = "/time?t=2018-09-14T23:20:08.000-07:00",
      andExpect = Ok,
      withJsonBody =
        """{"time": "2018-09-15 06:20:08 +0000"}""" // TimeStringSerializer format is 'yyyy-MM-dd HH:mm:ss Z' UTC TZ
    )
  }

  /**
   * Test that params are expected to be sent in the NamingStrategy of the ObjectMapper
   * (users should use the value field of the annotation to override)
   */
  test("/GET /longparametername") {
    server.httpGet(
      path =
        "/longparametername?this_should_use_mapper_naming_strategy=hello%2C%20world!", // default property naming strategy is snake_case
      andExpect = Ok,
      withBody = "hello, world!"
    )
  }

  /** Test annotated mix-in class converted by [[com.twitter.finatra.jackson.caseclass.CaseClassDeserializer]] */
  test("/GET /notmytype") {
    server.httpGet(
      path = "/notmytype?id=42&n=Item&d=This%20is%20an%20Item.",
      andExpect = Ok,
      withJsonBody = """{"description":"This is an Item.","id":42,"name":"Item"}"""
    )
  }

  /** Test annotated mix-in class converted by [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]] */
  test("/GET /thepoint") {
    server.httpGet(
      path = "/thepoint?p=%7B%22x%22%3A3%2C%22y%22%3A6%7D",
      andExpect = Ok,
      withJsonBody = """{"point":{"x":3,"y":6}}"""
    )
  }

  /** Test a JSON object is not parsed when the query param type is a String */
  test("/GET /complexquery") {
    server.httpGet(
      path =
        "/complexquery?q=%7B%22one%22%3A1%2C%22two%22%3A2%2C%22three%22%3A3%2C%22four%22%3A4%7D",
      andExpect = Ok,
      withJsonBody = """{"one":1,"two":2,"three":3,"four":4}"""
    )
  }

  /**
   * Test [[com.fasterxml.jackson.annotation.JsonTypeInfo]] classes converted by
   * [[com.twitter.finatra.http.internal.marshalling.MessageInjectableValues]]
   */
  test("/GET /shapeless") {
    // circle
    server.httpGet(
      path = "/shapeless?q=%7B%22type%22%3A%22circle%22%2C%22radius%22%3A10%7D",
      andExpect = Ok,
      withJsonBody = """{"shape":{"type":"circle","radius":10}}"""
    )

    // rectangle
    server.httpGet(
      path = "/shapeless?q=%7B%22type%22%3A%22rectangle%22%2C%22width%22%3A5%2C%22height%22%3A5%7D",
      andExpect = Ok,
      withJsonBody = """{"shape":{"type":"rectangle","width":5,"height":5}}"""
    )
  }

  /**
   * Test [[com.fasterxml.jackson.annotation.JsonTypeInfo]] classes converted by
   * [[com.twitter.finatra.jackson.caseclass.CaseClassDeserializer]]
   */
  test("/POST /shapes") {
    server.httpPost(
      path = "/shapes",
      postBody =
        """{"shapes":[{"type":"circle","radius":10},{"type":"rectangle","width":5,"height":5},{"type":"circle","radius":3},{"type":"rectangle","width":10,"height":5}]}""",
      andExpect = Ok,
      withJsonBody =
        """{"shapes":[{"radius":10},{"height":5,"width":5},{"radius":3},{"height":5,"width":10}]}"""
      // jsontypeinfo is not included in serialization by default
    )
  }

  /**
   * Deserialize Json to case class where some fields have
   * multiple [[com.twitter.finatra.validation.Constraint]] annotations
   *
   * The case class to be validated:
   * {{{
   *   case class ValidateUserRequest(
   *     @NotEmpty @Pattern(regexp = "[a-z]+") userName: String,
   *     @Max(value = 9999) id: Long,
   *     title: String
   *   )
   * }}}
   *
   */
  test("validation in case class deserialization failed") {
    server.httpPost(
      "/validate_user",
      andExpect = BadRequest,
      postBody = """
          {
            "user_name" : "@&^",
            "id" : "5656",
            "title": "CEO"
          }
        """"
    )
  }

  /**
   * Deserialize Json to case class where some fields have a [[com.twitter.finatra.validation.Constraint]]
   * annotation along with an HTTP annotation
   *
   * The case class to be validated:
   * {{{
   *   case class UserRequestWithParam(
   *     @QueryParam @Min(value = 1) userId: Long,
   *     @RouteParam @NotEmpty userName: String
   *   )
   * }}}
   */
  test("validation with HTTP annotations in request failed") {
    server.httpGet(
      "/validate_with_http_annotations/jack?user_id=0",
      andExpect = BadRequest
    )
  }
}
