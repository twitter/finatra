package com.twitter.finatra.test

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import org.jboss.netty.handler.codec.http.HttpMethod

class InMemoryHttpTest extends Test with Mockito {

  val inMemoryHttpService = new InMemoryHttpService()
  val mockResponse1 = mock[Response]
  val mockResponse2 = mock[Response]

  "single post mocked" in {
    inMemoryHttpService.mockPost("/foo", andReturn = mockResponse1)
    assertPost("/foo", "", mockResponse1)
  }

  "multiple posts mocked" in {
    inMemoryHttpService.mockPost("/foo", andReturn = mockResponse1)
    inMemoryHttpService.mockPost("/foo", andReturn = mockResponse2)

    assertPost("/foo", "", mockResponse1)
    assertPost("/foo", "", mockResponse2)
  }

  "post mocked with matching body" in {
    inMemoryHttpService.mockPost("/foo", withBody = "asdf", andReturn = mockResponse1)
    assertPost("/foo", "asdf", mockResponse1)
  }

  "post mocked with different body" in {
    inMemoryHttpService.mockPost("/foo", withBody = "asdf", andReturn = mockResponse1)

    val request = Request(HttpMethod.POST, "/foo")
    request.setContentString("11111")
    assertFailedFuture[Exception] {
      inMemoryHttpService.apply(request)
    }
  }

  def assertPost(path: String, body: String, response: Response) {
    val request = Request(HttpMethod.POST, path)
    request.setContentString(body)
    assertResponse(request, response)
  }

  def assertGet(path: String, body: String = "", response: Response) {
    val request = Request(HttpMethod.GET, path)
    assertResponse(request, response)
  }

  def assertResponse(request: Request, expectedResponse: Response) {
    val response = Await.result(inMemoryHttpService(request))
    if (response != expectedResponse) {
      fail(response + " does not equal expected " + expectedResponse)
    }
  }
}
