package com.twitter.finatra.http.tests.integration.darktraffic.test

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.http.modules.DarkTrafficFilterModule
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.integration.darktraffic.main.{
  DarkTrafficTestController,
  DarkTrafficTestServer
}
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer, HttpTest}
import com.twitter.inject.Test
import com.twitter.util.Future

class DarkTrafficCanonicalResourceHeaderTest extends Test with HttpTest {

  private[this] val assertCanonicalResourceHeaderFilter =
    new Filter[Request, Response, Request, Response] {
      def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        request.headerMap(DarkTrafficFilterModule.CanonicalResource) should be(
          s"${request.method.toString}_${request.uri}"
        )
        service(request)
      }
    }

  // receive dark traffic server
  private[this] val receiveDarkTrafficServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "receiveDarkTrafficServer"
      override protected def configureHttp(router: HttpRouter): Unit =
        router
          .filter(assertCanonicalResourceHeaderFilter)
          .add[DarkTrafficTestController]
    }
  )

  // send dark traffic server
  private[this] val sendDarkTrafficServer = new EmbeddedHttpServer(
    twitterServer = new DarkTrafficTestServer {
      override val name = "sendDarkTrafficServer"
    },
    flags = Map("http.dark.service.dest" -> receiveDarkTrafficServer.externalHttpHostAndPort)
  )

  override protected def afterAll(): Unit = {
    receiveDarkTrafficServer.close()
    sendDarkTrafficServer.close()
    super.afterAll()
  }

  // Canonical-Resource header is used by Diffy Proxy
  test("has 'Canonical-Resource' header correctly set") {
    sendDarkTrafficServer.httpGet("/plaintext", withBody = "Hello, World!", andExpect = Ok)
  }
}
