package com.twitter.finatra.streaming

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.benchmark.{FinatraBenchmarkServer, FinagleBenchmarkServer}
import com.twitter.inject.Test

class BenchmarkFeatureTest extends Test {

  val finatraServer = new EmbeddedHttpServer(new FinatraBenchmarkServer)
  val finagleServer = new EmbeddedHttpServer(new FinagleBenchmarkServer)

  "Servers" in {
    assertServers(
      path = "/json",
      withBody = """{"message":"Hello, World!"}""")

    assertServers(
      path = "/json/123",
      withBody = """{"message":"Hello 123"}""")

    assertServers(
      path = "/hi?name=Bob",
      withBody = "Hello Bob")
  }

  def assertServers(
    path: String,
    withBody: String) = {

    finatraServer.httpGet(
      path = path,
      withBody = withBody)

    finagleServer.httpGet(
      path = path,
      withBody = withBody)
  }

  override protected def afterAll() = {
    super.afterAll()
    finagleServer.close()
    finatraServer.close()
  }
}
