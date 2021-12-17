package com.twitter.finatra.http.tests.integration.darktraffic.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.darktraffic.main.DarkTrafficTestServer
import com.twitter.util.mock.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.server.PortUtils
import org.scalatest.concurrent.Eventually._

class DarkTrafficTestServerFeatureTest extends FeatureTest with Mockito {

  // receive dark traffic service
  override val server = new EmbeddedHttpServer(twitterServer = new DarkTrafficTestServer {
    override val name = "dark-server"
  })

  lazy val liveServer = new EmbeddedHttpServer(
    twitterServer = new DarkTrafficTestServer {
      override val name = "live-server"
    },
    flags = Map(
      "http.dark.service.dest" ->
        s"/$$/inet/${PortUtils.loopbackAddress}/${server.httpExternalPort()}"
    )
  )

  // See SampleDarkTrafficFilterModule#enableSampling
  test("DarkTrafficServer#Get method is forwarded") {
    liveServer.httpGet("/plaintext", andExpect = Ok, withBody = "Hello, World!")

    // service stats
    liveServer.inMemoryStats.counters.assert("route/plaintext/GET/status/200", 1)

    // darkTrafficFilter stats
    liveServer.inMemoryStats.counters.assert("dark_traffic_filter/forwarded", 1)
    liveServer.inMemoryStats.counters.get("dark_traffic_filter/skipped") should be(None)

    server.assertHealthy()
    // "dark" service stats
    eventually {
      server.inMemoryStats.counters.assert("route/plaintext/GET/status/200", 1)
    }
  }

  test("DarkTrafficServer#Put method is forwarded") {
    liveServer.httpPut(
      "/echo",
      putBody = "",
      andExpect = Ok,
      withBody = ""
    )

    // service stats
    liveServer.inMemoryStats.counters.assert("route/echo/PUT/status/200", 1)

    // darkTrafficFilter stats
    liveServer.inMemoryStats.counters.assert("dark_traffic_filter/forwarded", 1)
    liveServer.inMemoryStats.counters.get("dark_traffic_filter/skipped") should be(None)

    server.assertHealthy()
    // "dark" service stats
    eventually {
      server.inMemoryStats.counters.assert("route/echo/PUT/status/200", 1)
    }
  }

  test("DarkTrafficServer#Post method is forwarded") {
    liveServer.httpPost("/foo", postBody = """{"name":"bar"}""", andExpect = Ok, withBody = "bar")

    // service stats
    liveServer.inMemoryStats.counters.assert("route/foo/POST/status/200", 1)

    // darkTrafficFilter stats
    liveServer.inMemoryStats.counters.assert("dark_traffic_filter/forwarded", 1)
    liveServer.inMemoryStats.counters.get("dark_traffic_filter/skipped") should be(None)

    server.assertHealthy()
    // "dark" service stats
    eventually {
      server.inMemoryStats.counters.assert("route/foo/POST/status/200", 1)
    }
  }

  test("DarkTrafficServer#Delete method not forwarded") {
    liveServer.httpDelete("/delete", andExpect = Ok, withBody = "delete")

    // service stats
    liveServer.inMemoryStats.counters.assert("route/delete/DELETE/status/200", 1)

    // darkTrafficFilter stats
    liveServer.inMemoryStats.counters.get("dark_traffic_filter/forwarded") should be(None)
    liveServer.inMemoryStats.counters.assert("dark_traffic_filter/skipped", 1)

    server.assertHealthy()
    // "dark" service stats
    server.inMemoryStats.counters.get("route/delete/DELETE/status/200") should be(None)
  }

  override def beforeEach(): Unit = {
    liveServer.clearStats()
    server.clearStats()
    server.assertHealthy()
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    liveServer.close()
  }
}
