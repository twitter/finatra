package com.twitter.inject.thrift

import com.twitter.finagle.client.ClientRegistry
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.thrift.integration.serviceperendpoint.{EchoThriftMethodBuilderClientModule, EchoThriftService, GreeterThriftMethodBuilderClientModule, GreeterThriftService, ServicePerEndpointHttpController}
import com.twitter.inject.thrift.integration.{TestHttpServer, TestThriftServer}
import com.twitter.test.thriftscala.EchoService

class DoEverythingThriftMethodBuilderClientModuleFeatureTest extends FeatureTest with HttpTest {
  override val printStats = false

  private val httpServiceClientId = "http-service"

  private val greeterThriftServer = new EmbeddedThriftServer(
    twitterServer =
      new TestThriftServer(new GreeterThriftService(httpServiceClientId).toThriftService),
    disableTestLogging = true
  )

  private val echoThriftServer = new EmbeddedThriftServer(
    twitterServer = new TestThriftServer(new EchoThriftService(httpServiceClientId).toThriftService),
    disableTestLogging = true
  )

  // Test an HttpServer with multiple clients
  val server = new EmbeddedHttpServer(
    twitterServer = new TestHttpServer[ServicePerEndpointHttpController](
      "spe-server",
      GreeterThriftMethodBuilderClientModule,
      EchoThriftMethodBuilderClientModule
    ),
    args = Seq(
      s"-thrift.clientId=$httpServiceClientId",
      resolverMap(
        "greeter-thrift-service" -> greeterThriftServer.thriftHostAndPort,
        "echo-thrift-service" -> echoThriftServer.thriftHostAndPort
      )
    )
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    server.start()
  }

  override def afterAll(): Unit = {
    greeterThriftServer.close()
    echoThriftServer.close()
    super.afterAll()
  }

  test("Assert client stack registry entries") {
    // should only be one per thriftmethodbuilderclientmodule
    ClientRegistry.registrants.count(_.name == GreeterThriftMethodBuilderClientModule.label) should equal(1)
    ClientRegistry.registrants.count(_.name == EchoThriftMethodBuilderClientModule.label) should equal(1)
  }

  test("Greeter.ServicePerEndpoint is available from the injector") {
    server.injector.instance[Greeter.ServicePerEndpoint] should not be null
  }

  test("Greeter.MethodPerEndpoint is available from the injector") {
    server.injector.instance[Greeter.MethodPerEndpoint] should not be null
  }

  test("EchoService.ServicePerEndpoint is available from the injector") {
    server.injector.instance[EchoService.ServicePerEndpoint] should not be null
  }

  test("EchoService.MethodPerEndpoint is available from the injector") {
    server.injector.instance[EchoService.MethodPerEndpoint] should not be null
  }

  test("Say hi") {
    // fails more times (3) than the MethodBuilder number of retries (2).
    intercept[Exception] {
      server.httpGet(path = "/hi?name=Bob", andExpect = Ok, withBody = "Hi Bob")
    }
    // http server route stat
    server.assertStat("route/hi/GET/response_size", Seq(5))

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/requests", 3)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/success", 1)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hi/failures", 2)
    /* assert MethodBuilder stats exist */
    server.getStat("clnt/greeter-thrift-client/hi/logical/request_latency_ms") should not be Seq()
    server.getStat("clnt/greeter-thrift-client/hi/retries") should be(Seq(2.0))
    /* assert MethodBuilder counters */
    server.assertCounter("clnt/greeter-thrift-client/hi/logical/requests", 1)
    server.assertCounter("clnt/greeter-thrift-client/hi/logical/success", 1)
  }

  test("Say hello") {
    server.httpGet(path = "/hello?name=Bob", andExpect = Ok, withBody = "Hello Bob")
    // http server route stat
    server.assertStat("route/hello/GET/response_size", Seq(9))

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hello/requests", 3)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hello/success", 1)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/hello/failures", 2)
    /* assert MethodBuilder stats exist */
    server.getStat("clnt/greeter-thrift-client/hello/logical/request_latency_ms") should not be Seq()
    server.getStat("clnt/greeter-thrift-client/hello/retries") should be(Seq(2.0))
    /* assert MethodBuilder counters */
    server.assertCounter("clnt/greeter-thrift-client/hello/logical/requests", 1)
    server.assertCounter("clnt/greeter-thrift-client/hello/logical/success", 1)
  }

  test("Say bye") {
    server.httpGet(
      path = "/bye?name=Bob&age=18",
      andExpect = Ok,
      withBody = "Bye Bob of 18 years!"
    )
    // http server route stat
    server.assertStat("route/bye/GET/response_size", Seq(20))

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/requests", 3)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/success", 1)
    server.assertCounter("clnt/greeter-thrift-client/Greeter/bye/failures", 2)
    /* assert MethodBuilder stats exist */
    server.getStat("clnt/greeter-thrift-client/bye/logical/request_latency_ms") should not be Seq()
    server.getStat("clnt/greeter-thrift-client/bye/retries") should be(Seq(2.0))
    /* assert MethodBuilder counters */
    server.assertCounter("clnt/greeter-thrift-client/bye/logical/requests", 1)
    server.assertCounter("clnt/greeter-thrift-client/bye/logical/success", 1)
  }

  test("echo 3 times") {
    server.httpPost(
      path = "/config?timesToEcho=2",
      postBody = "",
      andExpect = Ok,
      withBody = "2"
    )

    server.httpPost(
      path = "/config?timesToEcho=3",
      postBody = "",
      andExpect = Ok,
      withBody = "3"
    )

    server.httpGet(path = "/echo?msg=Bob", andExpect = Ok, withBody = "BobBobBob")
    // http server route stats
    server.assertStat("route/config/POST/response_size", Seq(1, 1))
    server.assertStat("route/echo/GET/response_size", Seq(9))

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.assertCounter("clnt/echo-thrift-client/EchoService/echo/requests", 1)
    server.assertCounter("clnt/echo-thrift-client/EchoService/echo/success", 1)
    server.assertCounter("clnt/echo-thrift-client/EchoService/echo/failures", 0)
    /* assert MethodBuilder stats exist */
    server.getStat("clnt/echo-thrift-client/echo/logical/request_latency_ms") should not be Seq()
    server.getStat("clnt/echo-thrift-client/echo/retries") should be(Seq(0.0))
    /* assert MethodBuilder counters */
    server.assertCounter("clnt/echo-thrift-client/echo/logical/requests", 1)
    server.assertCounter("clnt/echo-thrift-client/echo/logical/success", 1)
  }
}
