package com.twitter.finatra.streaming

import com.twitter.finagle.http.Response
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.benchmark.{FinagleBenchmarkServer, FinatraBenchmarkServer}
import com.twitter.inject.Test

class BenchmarkFeatureTest extends Test {

  val finatraServer = new EmbeddedHttpServer(
    new FinatraBenchmarkServer,
    flags = Map("http.response.charset.enabled" -> "false")
  )
  val finagleServer = new EmbeddedHttpServer(new FinagleBenchmarkServer)

  test("Benchmark#Servers") {
    assertServers(
      path = "/",
      withContentType = "application/json",
      withBody = """{"message":"Hello, World!"}""",
      withContentLength = 27
    )

    assertServers(
      path = "/plaintext",
      withContentType = "text/plain",
      withBody = "Hello, World!",
      withContentLength = 13
    )
  }

  def assertServers(
    path: String,
    withContentType: String,
    withBody: String,
    withContentLength: Int
  ) = {

    assertHeaders(
      withContentType,
      "Finatra",
      contentLength = withContentLength,
      finatraServer.httpGet(path = path, withBody = withBody)
    )

    assertHeaders(
      withContentType,
      "Finagle",
      contentLength = withContentLength,
      finagleServer.httpGet(path = path, withBody = withBody)
    )
  }

  private def assertHeaders(
    contentType: String,
    server: String,
    contentLength: Int,
    response: Response
  ) = {
    response.headerMap.size should equal(4)
    response.headerMap.contains("Date")
    response.headerMap("Server") should equal(server)
    response.headerMap("Content-Type") should equal(contentType)
    response.headerMap("Content-Length") should equal(contentLength.toString)
  }

  override protected def afterAll() = {
    super.afterAll()
    finagleServer.close()
    finatraServer.close()
  }
}
