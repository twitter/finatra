package com.twitter.finatra.httpclient

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.httpclientv2.modules.HttpClientModule
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.util.Await

class HttpClientv2IntegrationTest extends IntegrationTest {

  val inMemoryHttpService = new InMemoryHttpService()

  override val injector: Injector =
    TestInjector(modules = Seq(MyHttpClientModuleV2, FinatraJacksonModule))
      .bind[Service[Request, Response]](inMemoryHttpService)
      .create

  val httpClient = injector.instance[HttpClient]

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

  test("executeJson w/ unexpected response") {
    val errorResponse = Response(Status.InternalServerError)
    inMemoryHttpService.mockGet("/foo", errorResponse)
    val request = RequestBuilder.get("/foo")

    intercept[HttpClientException] {
      Await.result(httpClient.executeJson[JsonNode](request))
    }
  }

  test("executeJson w/ expected response but unparsable body") {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockGet("/foo", mockResponse)
    val request = RequestBuilder.get("/foo")

    val e = intercept[HttpClientException] {
      Await.result(httpClient.executeJson[Int](request))
    }
    assert(e.getMessage.contains("com.fasterxml.jackson.databind.JsonMappingException"))
  }

  test("get") {
    val mockResponse = Response(Status.Ok)
    mockResponse.setContentString("{}")
    inMemoryHttpService.mockGet("/foo", mockResponse)

    Await.result(httpClient.get("/foo")) should be(mockResponse) //Purposely using deprecated method for test coverage
  }

  object MyHttpClientModuleV2 extends HttpClientModule {
    override val dest = "/my-http-service"
    override val hostname = "hostname123"
    override val defaultHeaders = Map("a" -> "b")
  }

}
