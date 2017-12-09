package com.twitter.finatra.thrift.tests

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.inheritance.InheritanceThriftTwitterServer
import com.twitter.inject.server.PortUtils
import com.twitter.io.Buf
import com.twitter.scrooge.{Request, Response}
import com.twitter.serviceA.thriftscala.ServiceA.Echo
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.serviceB.thriftscala.ServiceB.Ping
import com.twitter.util.Future

class InheritanceThriftServerReqRepTest extends ReqRepServicePerEndpointTest {
  private val server = new EmbeddedThriftServer(
    twitterServer = new InheritanceThriftTwitterServer(TestClientRequestHeaderKey)
  )

  private val muxCtxtsFilter = muxContextsFilter

  private val reqRepServicePerEndpoint: ServiceB.ReqRepServicePerEndpoint =
    ThriftMux.client
      .filtered(muxCtxtsFilter.toFilter)
      .configured(Stats(NullStatsReceiver))
      .withClientId(ClientId("client123"))
      .reqRepServicePerEndpoint[ServiceB.ReqRepServicePerEndpoint](
        PortUtils.loopbackAddressForPort(server.thriftPort), "client123")

  /* test that copy works for client */
  private val filteredReqRepServicePerEndpoint: ServiceB.ReqRepServicePerEndpoint =
    reqRepServicePerEndpoint
      .withPing(
        ping = specificMethodLoggingFilter.andThen(reqRepServicePerEndpoint.ping))
      .withEcho(reqRepServicePerEndpoint.echo)
    .filtered(globalLoggingFilter)

  override def beforeAll(): Unit = {
    server.start()
  }

  override def afterAll(): Unit = {
    server.close()
  }

  override def afterEach(): Unit = {
    muxCtxtsFilter.clear()
  }

  test("ping") {
    val request: Request[Ping.Args] =
      Request(Ping.Args())
        .setHeader(TestClientRequestHeaderKey, "bar")
        .setHeader("com.twitter.finagle.Retries", "5") // set a header that finagle already broadcast to ensure that we collect and don't blow up.
    val f: Future[Response[Ping.SuccessType]] =
      reqRepServicePerEndpoint.ping(request)
    val response = await(f)
    response.value should equal("pong")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.serviceb.thriftscala.serviceb.ping").head
    decodeBuf(buf) should equal("response")

    muxCtxtsFilter.contexts.isEmpty should be(false)
    muxCtxtsFilter.contexts.size should be(1)
    val (keyBuf, valBuf) = muxCtxtsFilter.contexts.head
    decodeBuf(keyBuf) should equal("com.twitter.serviceb.thriftscala.serviceb.ping")
    decodeBuf(valBuf) should equal("response")
  }

  test("filtered ping") {
    val request: Request[Ping.Args] =
      Request(
        headers = Map(
          TestClientRequestHeaderKey -> Seq(Buf.Utf8("bar")),
          "com.twitter.finagle.Retries" ->  Seq(Buf.Utf8("5"))), // set a header that finagle already broadcast to ensure that we collect and don't blow up.
        args = Ping.Args())
    val f: Future[Response[Ping.SuccessType]] =
      filteredReqRepServicePerEndpoint.ping(request)
    val response = await(f)
    response.value should equal("pong")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.serviceb.thriftscala.serviceb.ping").head
    decodeBuf(buf) should equal("response")
  }

  test("echo") {
    val request: Request[Echo.Args] =
      Request(
        headers = Map(TestClientRequestHeaderKey -> Seq(Buf.Utf8("bar"))),
        args = Echo.Args("Hello, World."))
    val f: Future[Response[Echo.SuccessType]] =
      reqRepServicePerEndpoint.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.servicea.thriftscala.servicea.echo").head
    decodeBuf(buf) should equal("response")
  }

  test("filtered echo") {
    val request: Request[Echo.Args] =
      Request(Echo.Args("Hello, World."))
        .setHeader(TestClientRequestHeaderKey, "bar")
    val f: Future[Response[Echo.SuccessType]] =
      filteredReqRepServicePerEndpoint.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.servicea.thriftscala.servicea.echo").head
    decodeBuf(buf) should equal("response")
  }

  test("echo[Future]") {
    // use ServicePerEndpoint which has the 'muxCtxtsFilter' ThriftMux client filter
    val reqRepMethodPerEndpoint: ServiceB[Future] =
      ThriftMux.client
        .reqRepMethodPerEndpoint(reqRepServicePerEndpoint)

    val f: Future[String] =
      reqRepMethodPerEndpoint.echo("Hello, World.")
    val response = await(f)
    response should equal("Hello, World.")

    muxCtxtsFilter.contexts.isEmpty should be(false)
    muxCtxtsFilter.contexts.size should be(1)
    val (keyBuf, valBuf) = muxCtxtsFilter.contexts.head
    decodeBuf(keyBuf) should equal("com.twitter.servicea.thriftscala.servicea.echo")
    decodeBuf(valBuf) should equal("response")
  }

  test("methodPerEndpoint echo") {
    val methodPerEndpointClient123: ServiceB.MethodPerEndpoint =
      ThriftMux.client
        .filtered(muxCtxtsFilter.toFilter)
        .newIface[ServiceB.MethodPerEndpoint](
          PortUtils.loopbackAddressForPort(server.thriftPort),
          "client123")

    val f: Future[Echo.SuccessType] =
      methodPerEndpointClient123.echo("Hello, World.")
    val response = await(f)
    response should equal("Hello, World.")

    muxCtxtsFilter.contexts.isEmpty should be(false)
    muxCtxtsFilter.contexts.size should be(1)
    val (keyBuf, valBuf) = muxCtxtsFilter.contexts.head
    decodeBuf(keyBuf) should equal("com.twitter.servicea.thriftscala.servicea.echo")
    decodeBuf(valBuf) should equal("response")
  }
}
