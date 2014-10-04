package com.twitter.finatra.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.twitter.finagle.Service
import com.twitter.finagle.http.{HttpMuxer, Response, Status, Request => FinagleRequest}
import com.twitter.finatra.conversions.json._
import com.twitter.finatra.guice.FinatraInjector
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.marshalling.{MessageBodyManager, MustacheService}
import com.twitter.finatra.response.ResponseBuilder
import com.twitter.finatra.twitterserver.routing.FileResolver
import com.twitter.logging.Logger
import java.lang.annotation.{Annotation => JavaAnnotation}
import java.util.logging.Level
import org.jboss.netty.handler.codec.http.HttpResponseStatus

object HttpTest {
  val testClientAppId = 12345L
}

trait HttpTest extends Test with TestMapper {

  protected val testClientAppId = HttpTest.testClientAppId

  override protected def beforeAll() {
    super.beforeAll()
    configFinagleLogging()
  }

  def configFinagleLogging() {
    val finagleLog = Logger("finagle")
    finagleLog.setLevel(Level.WARNING)
  }

  /**
   * Method to reset the state of the global TwitterServer singleton, HttpMuxer
   * NOTE: This also clears the stats endpoint (/admin/metrics.json), since that is "service loaded" before
   * the server starts...
   */
  def clearAdminHttpMuxer() {
    setPrivateField(HttpMuxer, "underlying", new HttpMuxer())
  }

  def assertResponse(response: Response, expectedStatus: HttpResponseStatus = Status.Ok, suppress: Boolean = false): Response = {
    assertStatus(response, expectedStatus, suppress)
    logLocationIfCreated(response)
    logResponseContent(response, suppress)
    response
  }

  private def assertStatus(response: Response, expectedStatus: HttpResponseStatus, suppress: Boolean) {
    if (response.status != expectedStatus) {
      if (!suppress) {
        warn(response + " " + response.contentString)
      }
      response.status should equal(expectedStatus)
    }
  }

  private def logLocationIfCreated(response: Response) {
    if (response.status == HttpResponseStatus.CREATED) {
      debug("Location = " + response.headers.get("Location"))
    }
  }

  private def logResponseContent(response: Response, suppress: Boolean) {
    if (!response.contentString.isEmpty && !suppress) {
      try {
        val jsonNode = mapper.parse[JsonNode](response.contentString)
        if (jsonNode.isObject || jsonNode.isArray)
          info("Response: " + response + " " + jsonNode.toPrettyJson)
        else
          info("Response: " + response + " " + response.contentString)
      } catch {
        case e: Throwable =>
          info("Response: " + response + " " + response.contentString)
      }
    }
  }

  /**
   * @return Location header
   */
  def assertCreated(response: Response, suppress: Boolean = false): String = {
    response.status should equal(HttpResponseStatus.CREATED)

    if (!response.contentString.isEmpty) {
      if (!suppress) {
        debug(response.contentString.toPrettyJson)
      }
    }

    response.headers.get("Location")
  }

  def assertOk(response: Response, suppress: Boolean = false) = {
    assertResponse(response, HttpResponseStatus.OK, suppress)
  }

  def assertNotFound(response: Response, suppress: Boolean = false) = {
    assertResponse(response, HttpResponseStatus.NOT_FOUND, suppress)
  }

  def assertBadRequest(response: Response, suppress: Boolean = false) = {
    assertResponse(response, HttpResponseStatus.BAD_REQUEST, suppress)
  }

  def assertServiceUnAvailable(response: Response, suppress: Boolean = false) = {
    assertResponse(response, HttpResponseStatus.SERVICE_UNAVAILABLE, suppress)
  }

  def inMemoryHttpService[Ann <: JavaAnnotation : Manifest](injector: FinatraInjector): InMemoryHttpService = {
    injector.instance[Service[FinagleRequest, Response], Ann].asInstanceOf[InMemoryHttpService]
  }

  def assertHealth(server: EmbeddedTwitterServer, healthy: Boolean = true) = {
    server.start()
    assert(server.twitterServer.httpAdminPort != 0)
    val expectedBody = if(healthy) "OK\n" else ""

    server.httpGet(
      "/health",
      routeToAdminServer = true,
      andExpect = Status.Ok,
      withBody = expectedBody)

    server
  }

  val NormalizedId = "0"

  def IdNormalizer(jsonNode: JsonNode): JsonNode = {
    val objNode = jsonNode.asInstanceOf[ObjectNode]
    objNode.put("id", NormalizedId)
    objNode
  }

  /* Add parseJson to HTTP Response */
  implicit def parseJsonToResponse(response: Response) = new {
    def parseJson[T: Manifest]: T = {
      response.withInputStream { inputStream =>
        mapper.parse[T](inputStream)
      }
    }
  }

  /* Test Responses */

  //NOTE: TestResponses may not be able to use all features provided by normally injected dependencies
  protected lazy val testResponseBuilder = new ResponseBuilder(
    mapper,
    new FileResolver,
    new MessageBodyManager(null, null, null),
    new MustacheService(null))

  def ok = testResponseBuilder.ok

  def ok(body: Any) = testResponseBuilder.ok.body(body)

  def created = testResponseBuilder.created

  def accepted = testResponseBuilder.accepted

  def forbidden = testResponseBuilder.forbidden

  def notFound = testResponseBuilder.notFound

  def internalServerError = testResponseBuilder.internalServerError

  def internalServerError(body: Any) = testResponseBuilder.internalServerError.body(body)

  def clientClosed = testResponseBuilder.clientClosed

  def response(statusCode: Int) = testResponseBuilder.status(statusCode)

  def response(httpResponseStatus: HttpResponseStatus) = testResponseBuilder.status(httpResponseStatus)
}