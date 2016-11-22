package com.twitter.finatra.http.tests.integration.darktraffic.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.darktraffic.main.DarkTrafficTestServer
import com.twitter.inject.Mockito
import com.twitter.inject.server.{FeatureTest, PortUtils}
import org.scalatest.concurrent.Eventually._

class DarkTrafficTestServerFeatureTest extends FeatureTest with Mockito {

  // receive dark traffic service
  override val server = new EmbeddedHttpServer(
    twitterServer = new DarkTrafficTestServer {
      override val name = "dark-server"
    })

  lazy val liveServer = new EmbeddedHttpServer(
    twitterServer = new DarkTrafficTestServer {
      override val name = "live-server"
    },
    flags =
      Map(
      "http.dark.service.dest" ->
        s"/$$/inet/${PortUtils.loopbackAddress}/${server.httpExternalPort}"))

  "DarkTrafficServer" should {

    // See SampleDarkTrafficFilterModule#enableSampling
    "Get method is forwarded" in {
      liveServer.httpGet(
        "/plaintext",
        andExpect = Ok,
        withBody = "Hello, World!")

      // service stats
      liveServer.assertCounter("route/plaintext/GET/status/200", 1)

      // darkTrafficFilter stats
      liveServer.assertCounter("dark_traffic_filter/forwarded", 1)
      liveServer.assertCounter("dark_traffic_filter/skipped", 0)

      server.assertHealthy()
      // "dark" service stats
      eventually {
        server.assertCounter("route/plaintext/GET/status/200", 1)
      }
    }

    "Put method is forwarded" in {
      liveServer.httpPut(
        "/echo",
        putBody = "",
        andExpect = Ok,
        withBody = ""
      )

      // service stats
      liveServer.assertCounter("route/echo/PUT/status/200", 1)

      // darkTrafficFilter stats
      liveServer.assertCounter("dark_traffic_filter/forwarded", 1)
      liveServer.assertCounter("dark_traffic_filter/skipped", 0)

      server.assertHealthy()
      // "dark" service stats
      eventually {
        server.assertCounter("route/echo/PUT/status/200", 1)
      }
    }

    "Post method not forwarded" in {
      liveServer.httpPost(
        "/foo",
        postBody = "",
        andExpect = Ok,
        withBody = "bar")

      // service stats
      liveServer.assertCounter("route/foo/POST/status/200", 1)

      // darkTrafficFilter stats
      liveServer.assertCounter("dark_traffic_filter/forwarded", 0)
      liveServer.assertCounter("dark_traffic_filter/skipped", 1)

      server.assertHealthy()
      // "dark" service stats
      server.assertCounter("route/foo/POST/status/200", 0)
    }

    "Delete method not forwarded" in {
      liveServer.httpDelete(
        "/delete",
        andExpect = Ok,
        withBody = "delete")

      // service stats
      liveServer.assertCounter("route/delete/DELETE/status/200", 1)

      // darkTrafficFilter stats
      liveServer.assertCounter("dark_traffic_filter/forwarded", 0)
      liveServer.assertCounter("dark_traffic_filter/skipped", 1)

      server.assertHealthy()
      // "dark" service stats
      server.assertCounter("route/delete/DELETE/status/200", 0)
    }
  }

  override def beforeEach(): Unit = {
    liveServer.clearStats()
    server.clearStats()
  }

  override def afterAll(): Unit = {
    liveServer.close()
  }
}