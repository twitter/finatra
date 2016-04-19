package com.twitter.finatra.httpclient

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.httpclient.test.{InMemoryHttpService, PostRequestWithIncorrectBodyException}
import com.twitter.inject.Test
import com.twitter.util.Await
import org.specs2.mock.Mockito

class InMemoryHttpServiceTest extends Test with Mockito {
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

    val request = Request(Method.Post, "/foo")
    request.setContentString("11111")
    assertFailedFuture[PostRequestWithIncorrectBodyException] {
      inMemoryHttpService.apply(request)
    }
  }

  def assertPost(path: String, body: String, response: Response) {
    val request = Request(Method.Post, path)
    request.setContentString(body)
    assertResponse(request, response)
  }

  def assertGet(path: String, body: String = "", response: Response) {
    val request = Request(Method.Get, path)
    assertResponse(request, response)
  }

  def assertResponse(request: Request, expectedResponse: Response) {
    val response = Await.result(inMemoryHttpService(request))
    if (response != expectedResponse) {
      fail(response + " does not equal expected " + expectedResponse)
    }
  }

  override protected def afterAll() = {
    super.afterAll()
    inMemoryHttpService.close()
  }
}
