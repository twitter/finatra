package com.twitter.inject.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status._
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.inject.thrift.DoEverythingThriftClientModuleFeatureTest._
import com.twitter.inject.thrift.modules.{ThriftClientIdModule, ThriftClientModule}
import com.twitter.inject.{Logging, Test}
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.{Await, Future}
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}

object DoEverythingThriftClientModuleFeatureTest {
  class EchoHttpController1 @Inject()(echoThriftService: EchoService[Future]) extends Controller {
    get("/echo") { request: Request =>
      val msg = request.params("msg")
      echoThriftService.echo(msg)
    }
    post("/config") { request: Request =>
      val timesToEcho = request.params("timesToEcho").toInt
      echoThriftService.setTimesToEcho(timesToEcho)
    }
  }

  class EchoHttpController2 @Inject()(echoThriftService: EchoService.FutureIface) extends Controller {
    get("/echo") { request: Request =>
      val msg = request.params("msg")
      echoThriftService.echo(msg)
    }
    post("/config") { request: Request =>
      val timesToEcho = request.params("timesToEcho").toInt
      echoThriftService.setTimesToEcho(timesToEcho)
    }
  }

  object EchoThriftClientModule1
    extends ThriftClientModule[EchoService[Future]] {
    override val label = "echo-service"
    override val dest = "flag!thrift-echo-service"
  }

  object EchoThriftClientModule2
    extends ThriftClientModule[EchoService.FutureIface] {
    override val label = "echo-service"
    override val dest = "flag!thrift-echo-service"
  }

  @Singleton
  class MyEchoService extends EchoService[Future] with Logging {
    private val timesToEcho = new AtomicInteger(1)

    /* Public */
    override def echo(msg: String): Future[String] = {
      info("echo " + msg)
      assertClientId("echo-http-service")
      Future.value(msg * timesToEcho.get)
    }
    override def setTimesToEcho(times: Int): Future[Int] = {
      info("setTimesToEcho " + times)
      assertClientId("echo-http-service")
      timesToEcho.set(times) //mutation
      Future(times)
    }

    /* Private */
    private def assertClientId(name: String): Unit = {
      assert(ClientId.current.contains(ClientId(name)), "Invalid Client ID: " + ClientId.current)
    }
  }

  class EchoThriftServer extends TwitterServer {
    private val thriftPortFlag = flag("thrift.port", ":0", "External Thrift server port")
    private val thriftShutdownTimeout = flag(
      "thrift.shutdown.time",
      1.minute,
      "Maximum amount of time to wait for pending requests to complete on shutdown"
    )
    /* Private Mutable State */
    private var thriftServer: ListeningServer = _

    /* Lifecycle */
    override def postWarmup() {
      super.postWarmup()
      thriftServer = ThriftMux.server.serveIface(thriftPortFlag(), injector.instance[MyEchoService])
      info("Thrift server started on port: " + thriftPort.get)
    }

    onExit {
      Await.result(thriftServer.close(thriftShutdownTimeout().fromNow))
    }

    /* Overrides */
    override def thriftPort = Option(thriftServer) map PortUtils.getPort
  }
}

class DoEverythingThriftClientModuleFeatureTest extends Test with HttpTest {
  val thriftServer =
    new EmbeddedThriftServer(
      twitterServer = new EchoThriftServer)

  val httpServer1 = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "echo-http-server1"
      override val modules = Seq(ThriftClientIdModule, EchoThriftClientModule1)
      override def configureHttp(router: HttpRouter) {
        router.filter[CommonFilters].add[EchoHttpController1]
      }
    },
    args = Seq(
      "-thrift.clientId=echo-http-service",
      resolverMap("thrift-echo-service" -> thriftServer.thriftHostAndPort)
    )
  )

  val httpServer2 = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "echo-http-server2"
      override val modules = Seq(ThriftClientIdModule, EchoThriftClientModule2)
      override def configureHttp(router: HttpRouter) {
        router.filter[CommonFilters].add[EchoHttpController2]
      }
    },
    args = Seq(
      "-thrift.clientId=echo-http-service",
      resolverMap("thrift-echo-service" -> thriftServer.thriftHostAndPort)
    )
  )

  override def afterAll(): Unit = {
    thriftServer.close()
    httpServer1.close()
    httpServer2.close()
    super.afterAll()
  }

  // test EchoService[Future]
  test("EchoHttpServer1#echo 3 times") {
    httpServer1.httpPost(
      path = "/config?timesToEcho=2",
      postBody = "",
      andExpect = Ok,
      withBody = "2"
    )

    httpServer1.httpPost(
      path = "/config?timesToEcho=3",
      postBody = "",
      andExpect = Ok,
      withBody = "3"
    )

    httpServer1.httpGet(path = "/echo?msg=Bob", andExpect = Ok, withBody = "BobBobBob")
    httpServer1.assertStat("route/config/POST/response_size", Seq(1, 1))
    httpServer1.assertStat("route/echo/GET/response_size", Seq(9))
  }

  // test EchoService.FutureIface
  test("EchoHttpServer2#echo 3 times") {
    httpServer2.httpPost(
      path = "/config?timesToEcho=2",
      postBody = "",
      andExpect = Ok,
      withBody = "2"
    )

    httpServer2.httpPost(
      path = "/config?timesToEcho=3",
      postBody = "",
      andExpect = Ok,
      withBody = "3"
    )

    httpServer2.httpGet(path = "/echo?msg=Bob", andExpect = Ok, withBody = "BobBobBob")
    httpServer2.assertStat("route/config/POST/response_size", Seq(1, 1))
    httpServer2.assertStat("route/echo/GET/response_size", Seq(9))
  }
}
