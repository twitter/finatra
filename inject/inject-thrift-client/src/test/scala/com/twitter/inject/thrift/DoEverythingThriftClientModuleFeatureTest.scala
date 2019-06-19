package com.twitter.inject.thrift

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status._
import com.twitter.finagle.ServiceClosedException
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.thrift.DoEverythingThriftClientModuleFeatureTest._
import com.twitter.inject.thrift.integration.basic.{EchoThriftClientModule1, EchoThriftClientModule2, EchoThriftClientModule3, MyEchoService}
import com.twitter.inject.thrift.integration.{TestHttpServer, TestThriftServer}
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

object DoEverythingThriftClientModuleFeatureTest {
  @Singleton class EchoServiceFuture @Inject()(val service: EchoService[Future])

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

  /* Testing deprecated code for coverage */
  class EchoHttpController2 @Inject()(echoThriftService: EchoService.FutureIface)
      extends Controller {
    get("/echo") { request: Request =>
      val msg = request.params("msg")
      echoThriftService.echo(msg)
    }
    post("/config") { request: Request =>
      val timesToEcho = request.params("timesToEcho").toInt
      echoThriftService.setTimesToEcho(timesToEcho)
    }
  }

  class EchoHttpController3 @Inject()(echoThriftService: EchoService.MethodPerEndpoint)
      extends Controller {
    get("/echo") { request: Request =>
      val msg = request.params("msg")
      echoThriftService.echo(msg)
    }
    post("/config") { request: Request =>
      val timesToEcho = request.params("timesToEcho").toInt
      echoThriftService.setTimesToEcho(timesToEcho)
    }
  }
}

class DoEverythingThriftClientModuleFeatureTest extends FeatureTest with HttpTest {

  private val clientIdString = "echo-http-service"

  override val server =
    new EmbeddedThriftServer(twitterServer = new TestThriftServer(new MyEchoService), disableTestLogging = true)

  private val httpServer1 = new EmbeddedHttpServer(
    twitterServer =
      new TestHttpServer[EchoHttpController1]("echo-http-server1", EchoThriftClientModule1),
    args = Seq(
      s"-thrift.clientId=$clientIdString",
      resolverMap("thrift-echo-service" -> server.thriftHostAndPort)
    )
  )

  private val httpServer2 = new EmbeddedHttpServer(
    twitterServer =
      new TestHttpServer[EchoHttpController2]("echo-http-server2", EchoThriftClientModule2),
    args = Seq(
      s"-thrift.clientId=$clientIdString",
      resolverMap("thrift-echo-service" -> server.thriftHostAndPort)
    )
  )

  private val httpServer3 = new EmbeddedHttpServer(
    twitterServer =
      new TestHttpServer[EchoHttpController3]("echo-http-server3", EchoThriftClientModule3),
    args = Seq(
      s"-thrift.clientId=$clientIdString",
      resolverMap("thrift-echo-service" -> server.thriftHostAndPort)
    )
  )

  override def afterAll(): Unit = {
    httpServer1.close()
    httpServer2.close()
    httpServer3.close()
    super.afterAll()
  }

  // test EchoService[Future]
  test("EchoService[Future] is available from the injector") {
    httpServer1.injector.instance[EchoServiceFuture].service should not be null
  }
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
  }

  // test EchoService.FutureIface
  test("EchoService.FutureIface is available from the injector") {
    httpServer2.injector.instance[EchoService.FutureIface] should not be null
  }
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
  }

  // test EchoService.MethodPerEndpoint
  test("EchoService.MethodPerEndpoint is available from the injector") {
    httpServer3.injector.instance[EchoService.MethodPerEndpoint] should not be null
  }
  test("EchoHttpServer3#echo 3 times") {
    httpServer3.httpPost(
      path = "/config?timesToEcho=2",
      postBody = "",
      andExpect = Ok,
      withBody = "2"
    )

    httpServer3.httpPost(
      path = "/config?timesToEcho=3",
      postBody = "",
      andExpect = Ok,
      withBody = "3"
    )

    httpServer3.httpGet(path = "/echo?msg=Bob", andExpect = Ok, withBody = "BobBobBob")
  }

  test("ThriftClient#asClosable") {
    val thriftClient = server
      .thriftClient[EchoService[Future]](clientIdString)

    await(thriftClient.setTimesToEcho(2))
    await(thriftClient.setTimesToEcho(3))

    assertFutureValue(thriftClient.echo("Bob"), "BobBobBob")

    await(thriftClient.asClosable.close())
    intercept[ServiceClosedException] {
      await(thriftClient.echo("Bob"))
    }
  }
}
