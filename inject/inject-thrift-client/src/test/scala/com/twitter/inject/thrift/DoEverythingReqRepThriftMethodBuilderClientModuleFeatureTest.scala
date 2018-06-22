package com.twitter.inject.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.greeter.thriftscala.Greeter
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.thrift.DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest._
import com.twitter.inject.thrift.integration.reqrepserviceperendpoint.{ReqRepServicePerEndpointHttpController, GreeterReqRepThriftMethodBuilderClientModule, GreeterThriftService}
import com.twitter.inject.thrift.integration.{TestHttpServer, TestThriftServer}
import com.twitter.util.Duration
import com.twitter.util.tunable.Tunable

object DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest {
  case class HelloHeaders(empty: Boolean)
  case class HelloResponse(value: String, headers: HelloHeaders)
}

class DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest
  extends FeatureTest
  with HttpTest {
  override val printStats = false

  private val requestHeaderKey = "com.twitter.greeter.test.header"
  private val httpServiceClientId = "http-service"
  private val perRequestTimeoutTunable: Tunable[Duration] = Tunable.mutable("per-request", 50.millis)

  private val greeterThriftServer = new EmbeddedThriftServer(
    twitterServer = new TestThriftServer(
      new GreeterThriftService(
        httpServiceClientId,
        requestHeaderKey).toThriftService)
  )

  override val server = new EmbeddedHttpServer(
    twitterServer = new TestHttpServer[ReqRepServicePerEndpointHttpController](
      "rrspe-server",
      new GreeterReqRepThriftMethodBuilderClientModule(requestHeaderKey, perRequestTimeoutTunable)),
    args = Seq(
      s"-thrift.clientId=$httpServiceClientId",
      resolverMap(
        "greeter-thrift-service" -> greeterThriftServer.thriftHostAndPort)
    )
  )

  override def afterAll(): Unit = {
    greeterThriftServer.close()
    super.afterAll()
  }

  test("Greeter.ReqRepServicePerEndpoint is available from the injector") {
    server.injector.instance[Greeter.ReqRepServicePerEndpoint] should not be null
  }

  test("Greeter.MethodPerEndpoint is available from the injector") {
    server.injector.instance[Greeter.MethodPerEndpoint] should not be null
  }

  test("Say hi") {
    // fails more times (3) than the MethodBuilder number of retries (2).
    intercept[Exception] {
      server.httpGet(path = "/hi?name=Bob", andExpect = Ok, withBody = "Hi Bob")
    }

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServiceIface#statsFilter */
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
    val response =
      server.httpGetJson[HelloResponse](path = "/hello?name=Bob", andExpect = Ok)
    assert(response.value == "Hello Bob")
    assert(!response.headers.empty)

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServiceIface#statsFilter */
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

    // per-method -- all the requests in this test were to the same method
    /* assert counters added by ThriftServiceIface#statsFilter */
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
}
