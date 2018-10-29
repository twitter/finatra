package com.twitter.finatra.httpclient

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.inject.app.TestInjector
import com.twitter.util.Await

class HttpClientIntegrationTest extends IntegrationTest {

  private[this] val inMemoryHttpService = new InMemoryHttpService()

  override val injector: Injector =
    TestInjector(modules = Seq(MyHttpClientModule, FinatraJacksonModule))
      .bind[Service[Request, Response]].toInstance(inMemoryHttpService)
      .create

  private[this] val httpClient = injector.instance[HttpClient]

  override def afterEach(): Unit = {
    resetResettables(inMemoryHttpService)
  }

  test("execute") {
    val okResponse = Response(Status.Ok)
    inMemoryHttpService.mockGet("/foo", okResponse)
    val request = RequestBuilder.get("/foo")

    Await.result(httpClient.execute(request)) should be(okResponse)
  }

  test("executeJson") {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockPost("/foo", "bar", mockResponse)

    val request = RequestBuilder.post("/foo").body("bar")

    Await.result(httpClient.executeJson[JsonNode](request)).toString should be("{}")
  }

  test("executeJson#unexpected response") {
    val errorResponse = Response(Status.InternalServerError)
    inMemoryHttpService.mockGet("/foo", errorResponse)
    val request = RequestBuilder.get("/foo")

    intercept[HttpClientException] {
      Await.result(httpClient.executeJson[JsonNode](request))
    }
  }

  test("executeJson#expected response but un-parsable body") {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockGet("/foo", mockResponse)
    val request = RequestBuilder.get("/foo")

    val e = intercept[HttpClientException] {
      Await.result(httpClient.executeJson[Int](request))
    }
    assert(e.getMessage.contains("com.fasterxml.jackson.databind.exc.MismatchedInputException"))
  }

  test("get#deprecated") {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockGet("/foo", mockResponse)

    Await.result(httpClient.get("/foo")) should be(mockResponse) //Purposely using deprecated method for test coverage
  }

  object MyHttpClientModule extends HttpClientModule {
    override val dest = "/my-http-service"
    override val hostname = "hostname123"
    override val defaultHeaders = Map("a" -> "b")
  }
}
