package com.twitter.finatra.httpclient

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Provides
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.httpclient.modules.HttpClientModuleTrait
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.util.Await
import com.twitter.util.jackson.ScalaObjectMapper
import javax.inject.Singleton

class HttpClientIntegrationTest extends IntegrationTest {

  private[this] val inMemoryHttpService = new InMemoryHttpService()

  override val injector: Injector =
    TestInjector(modules = Seq(MyHttpClientModule, ScalaObjectMapperModule))
      .bind[Service[Request, Response]].toInstance(inMemoryHttpService)
      .create

  private[this] val httpClient = injector.instance[HttpClient]

  override def afterEach(): Unit = {
    inMemoryHttpService.reset()
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

  object MyHttpClientModule extends HttpClientModuleTrait {
    override val dest = "/my-http-service"
    override val label = "test-client"
    override val hostname = "hostname123"
    override val defaultHeaders = Map("a" -> "b")

    @Singleton
    @Provides
    def providesHttpClient(
      mapper: ScalaObjectMapper,
      service: Service[Request, Response]
    ): HttpClient = new HttpClient(
      hostname = hostname,
      httpService = service,
      retryPolicy = retryPolicy,
      defaultHeaders = defaultHeaders,
      mapper = mapper
    )
  }

}
