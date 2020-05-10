package com.twitter.finatra.http.tests.server

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServerTrait}
import com.twitter.inject.Test
import com.twitter.util.Future
import java.net.InetSocketAddress

class HttpServerTraitIntegrationTest extends Test {

  test("HiServiceServer#respond") {
    val server = new EmbeddedHttpServer(new HiServer)
    try {
      server.httpGet("/asdf", andExpect = Status.Ok, withBody = "hi")
    } finally {
      server.close()
    }
  }

  test("HttpServerTrait test if ListeningServer http address is bound in postWarmup") {
    var httpBoundAddressBeforePostWarmup: Option[InetSocketAddress] = None
    var httpBoundAddressPostWarmup: Option[InetSocketAddress] = None
    var httpsBoundAddressPostWarmup: Option[InetSocketAddress] = None

    val server = new EmbeddedHttpServer(new HttpServerTrait {
      override protected def httpService: Service[Request, Response] =
        Service.const[Response](Future.value(Response()))

      override protected def postInjectorStartup(): Unit = {
        super.postInjectorStartup()
        httpBoundAddressBeforePostWarmup = httpBoundAddress
      }

      override protected def postWarmup(): Unit = {
        super.postWarmup()
        httpBoundAddressPostWarmup = httpBoundAddress
        httpsBoundAddressPostWarmup = httpsBoundAddress
      }
    })

    try {
      server.start()
      // Testing value of http boundAddress before postWarmup and after postWarmup
      assert(httpBoundAddressBeforePostWarmup.isEmpty)
      assert(httpBoundAddressPostWarmup.isDefined)

      // Test if the https address is not bound since we start the server on http port
      assert(httpsBoundAddressPostWarmup.isEmpty)
    } finally {
      server.close()
    }
  }

  test("HttpServerTrait test if ListeningServer https address is bound in postWarmup") {
    var httpsBoundAddressBeforePostWarmup: Option[InetSocketAddress] = None
    var httpsBoundAddressPostWarmup: Option[InetSocketAddress] = None

    val httpsPortFlag = "https.port"

    val server = new EmbeddedHttpServer(
      new HttpServerTrait {
        override protected def httpService: Service[Request, Response] =
          Service.const[Response](Future.value(Response()))

        override protected def postInjectorStartup(): Unit = {
          super.postInjectorStartup()
          httpsBoundAddressBeforePostWarmup = httpsBoundAddress
        }

        override protected def postWarmup(): Unit = {
          super.postWarmup()
          httpsBoundAddressPostWarmup = httpsBoundAddress
        }
      },
      httpPortFlag = httpsPortFlag)

    try {
      server.start()
      // Testing value of http boundAddress before postWarmup and after postWarmup
      assert(httpsBoundAddressBeforePostWarmup.isEmpty)
      assert(httpsBoundAddressPostWarmup.isDefined)
    } finally {
      server.close()
    }
  }
}

class HiServer extends HttpServerTrait {

  /** Override with an implementation to serve an HTTP Service */
  override protected def httpService: Service[Request, Response] = new Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response()
      response.statusCode(200)
      response.setContentString("hi")
      Future(response)
    }
  }
}
