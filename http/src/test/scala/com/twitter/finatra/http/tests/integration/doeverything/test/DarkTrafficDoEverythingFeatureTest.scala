package com.twitter.finatra.http.tests.integration.doeverything.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingServer
import com.twitter.inject.server.PortUtils


class DarkTrafficDoEverythingFeatureTest extends HttpTest {

  val darkDoEverythingHttpServer = new EmbeddedHttpServer( new DoEverythingServer {
    override val name = "dark-server"
  })
  val liveDoEverythingHttpServer = new EmbeddedHttpServer(
    new DoEverythingServer {
      override val name = "live-server"
    },
    flags = Map(
      "http.dark.service.dest" ->
        s"/$$/inet/${PortUtils.loopbackAddress}/${darkDoEverythingHttpServer.httpExternalPort}"))

  // See DoEverythingHttpServerDarkTrafficFilterModule#enableSampling
  "DoEverythingServer" should {

    "Get method is forwarded" in {
      liveDoEverythingHttpServer.httpGet(
        "/plaintext",
        withBody = "Hello, World!")

      // service stats
      liveDoEverythingHttpServer.assertCounter("route/plaintext/GET/status/200", 1)

      // darkTrafficFilter stats
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/forwarded", 1)
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/skipped", 0)

      darkDoEverythingHttpServer.assertHealthy() // stat to be recorded on the dark service
      // "dark" service stats
      darkDoEverythingHttpServer.assertCounter("route/plaintext/GET/status/200", 1)
    }

    "Put method is forwarded" in {
      liveDoEverythingHttpServer.httpPut(
        "/echo",
        putBody = "",
        andExpect = Ok,
        withBody = ""
      )

      // service stats
      liveDoEverythingHttpServer.assertCounter("route/echo/PUT/status/200", 1)

      // darkTrafficFilter stats
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/forwarded", 1)
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/skipped", 0)

      darkDoEverythingHttpServer.assertHealthy() // stat to be recorded on the dark service
      // "dark" service stats
      darkDoEverythingHttpServer.assertCounter("route/echo/PUT/status/200", 1)
    }

    "Post method not forwarded" in {
      liveDoEverythingHttpServer.httpPost(
        "/foo",
        postBody = "",
        andExpect = Ok,
        withBody = "bar")

      // service stats
      liveDoEverythingHttpServer.assertCounter("route/foo/POST/status/200", 1)

      // darkTrafficFilter stats
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/forwarded", 0)
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/skipped", 1)

      darkDoEverythingHttpServer.assertHealthy() // stat to be recorded on the dark service
      // "dark" service stats
      darkDoEverythingHttpServer.assertCounter("route/foo/POST/status/200", 0)
    }

    "Delete method not forwarded" in {
      liveDoEverythingHttpServer.httpDelete(
        "/delete",
        andExpect = Ok,
        withBody = "delete")

      // service stats
      liveDoEverythingHttpServer.assertCounter("route/delete/DELETE/status/200", 1)

      // darkTrafficFilter stats
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/forwarded", 0)
      liveDoEverythingHttpServer.assertCounter("dark_traffic_filter/skipped", 1)

      darkDoEverythingHttpServer.assertHealthy() // stat to be recorded on the dark service
      // "dark" service stats
      darkDoEverythingHttpServer.assertCounter("route/delete/DELETE/status/200", 0)
    }
  }

  override protected def beforeEach(): Unit = {
    darkDoEverythingHttpServer.clearStats()
    liveDoEverythingHttpServer.clearStats()
  }

  override def afterAll(): Unit = {
    darkDoEverythingHttpServer.close()
    liveDoEverythingHttpServer.close()
  }
}