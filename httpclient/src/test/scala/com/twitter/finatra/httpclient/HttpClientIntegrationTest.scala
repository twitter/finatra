package com.twitter.finatra.httpclient

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.testing.fieldbinder.Bind
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector
import com.twitter.util.Await

class HttpClientIntegrationTest extends IntegrationTest {

  val inMemoryHttpService = new InMemoryHttpService()

  @Bind
  val httpService: Service[Request, Response] = inMemoryHttpService

  override val injector = TestInjector(
    modules = Seq(MyHttpClientModule, FinatraJacksonModule),
    overrideModules = Seq(integrationTestModule))

  val httpClient = injector.instance[HttpClient]

  "execute" in {
    val okResponse = Response(Status.Ok)
    inMemoryHttpService.mockGet("/foo", okResponse)
    val request = RequestBuilder.get("/foo")

    Await.result(
      httpClient.execute(request)) should be(okResponse)
  }

  "executeJson" in {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockPost("/foo", "bar", mockResponse)

    val request = RequestBuilder.post("/foo").body("bar")

    Await.result(
      httpClient.executeJson[JsonNode](request)).toString should be("{}")
  }

  "executeJson w/ unexpected response" in {
    val errorResponse = Response(Status.InternalServerError)
    inMemoryHttpService.mockGet("/foo", errorResponse)
    val request = RequestBuilder.get("/foo")

    intercept[HttpClientException] {
      Await.result(
        httpClient.executeJson[JsonNode](request))
    }
  }

  "executeJson w/ expected response but unparsable body" in {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockGet("/foo", mockResponse)
    val request = RequestBuilder.get("/foo")

    val e = intercept[HttpClientException] {
      Await.result(
        httpClient.executeJson[Int](request))
    }
    assert(e.getMessage.contains("com.fasterxml.jackson.databind.JsonMappingException"))
  }

  "get" in {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockGet("/foo", mockResponse)

    Await.result(
      httpClient.get("/foo")) should be(mockResponse) //Purposely using deprecated method for test coverage
  }

  object MyHttpClientModule extends HttpClientModule {
    override val dest = "/my-http-service"
    override val hostname = "hostname123"
    override val defaultHeaders = Map("a" -> "b")
  }

}
