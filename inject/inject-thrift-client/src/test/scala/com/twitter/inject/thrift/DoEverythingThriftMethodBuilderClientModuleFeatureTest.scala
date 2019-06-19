package com.twitter.inject.thrift

import com.twitter.finagle.client.ClientRegistry
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.thrift.integration.serviceperendpoint.{
  EchoThriftMethodBuilderClientModule,
  EchoThriftService,
  GreeterThriftMethodBuilderClientModule,
  GreeterThriftService,
  ServicePerEndpointHttpController
}
import com.twitter.inject.thrift.integration.{TestHttpServer, TestThriftServer}
import com.twitter.test.thriftscala.EchoService

class DoEverythingThriftMethodBuilderClientModuleFeatureTest extends FeatureTest with HttpTest {
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
    ClientRegistry.registrants.count(_.name == GreeterThriftMethodBuilderClientModule.label) should equal(
      1)
    ClientRegistry.registrants.count(_.name == EchoThriftMethodBuilderClientModule.label) should equal(
      1)
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
    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/hi/requests", 3)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/hi/success", 1)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/hi/failures", 2)
    /* assert MethodBuilder stats exist */
    server.inMemoryStats.stats
      .get("clnt/greeter-thrift-client/hi/logical/request_latency_ms") should not be None
    server.inMemoryStats.stats
      .assert("clnt/greeter-thrift-client/hi/retries", Seq(2.0f))
    /* assert MethodBuilder counters */
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/hi/logical/requests", 1)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/hi/logical/success", 1)
  }

  test("Say hello") {
    server.httpGet(path = "/hello?name=Bob", andExpect = Ok, withBody = "Hello Bob")
    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/hello/requests", 3)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/hello/success", 1)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/hello/failures", 2)
    /* assert MethodBuilder stats exist */
    server.inMemoryStats.stats
      .get("clnt/greeter-thrift-client/hello/logical/request_latency_ms") should not be None
    server.inMemoryStats.stats
      .assert("clnt/greeter-thrift-client/hello/retries", Seq(2.0f))
    /* assert MethodBuilder counters */
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/hello/logical/requests", 1)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/hello/logical/success", 1)
  }

  test("Say bye") {
    server.httpGet(
      path = "/bye?name=Bob&age=18",
      andExpect = Ok,
      withBody = "Bye Bob of 18 years!"
    )
    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/bye/requests", 3)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/bye/success", 1)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/Greeter/bye/failures", 2)
    /* assert MethodBuilder stats exist */
    server.inMemoryStats.stats
      .get("clnt/greeter-thrift-client/bye/logical/request_latency_ms") should not be None
    server.inMemoryStats.stats
      .assert("clnt/greeter-thrift-client/bye/retries", Seq(2.0f))
    /* assert MethodBuilder counters */
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/bye/logical/requests", 1)
    server.inMemoryStats.counters
      .assert("clnt/greeter-thrift-client/bye/logical/success", 1)
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
    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServicePerEndpoint#statsFilter */
    server.inMemoryStats.counters
      .assert("clnt/echo-thrift-client/EchoService/echo/requests", 1)
    server.inMemoryStats.counters
      .assert("clnt/echo-thrift-client/EchoService/echo/success", 1)
    server.inMemoryStats.counters
      .get("clnt/echo-thrift-client/EchoService/echo/failures") should be(None)
    /* assert MethodBuilder stats exist */
    server.inMemoryStats.stats
      .get("clnt/echo-thrift-client/echo/logical/request_latency_ms") should not be None
    server.inMemoryStats.stats
      .assert("clnt/echo-thrift-client/echo/retries", Seq(0.0f))
    /* assert MethodBuilder counters */
    server.inMemoryStats.counters
      .assert("clnt/echo-thrift-client/echo/logical/requests", 1)
    server.inMemoryStats.counters
      .assert("clnt/echo-thrift-client/echo/logical/success", 1)
  }
}
