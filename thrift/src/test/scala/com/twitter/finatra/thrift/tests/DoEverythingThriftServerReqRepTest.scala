package com.twitter.finatra.thrift.tests

import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.doeverything.thriftscala.DoEverything.{Echo, MagicNum, Uppercase}
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftTwitterServer
import com.twitter.inject.server.PortUtils
import com.twitter.io.Buf
import com.twitter.scrooge.{Request, Response}
import com.twitter.util.Future

class DoEverythingThriftServerReqRepTest extends ReqRepServicePerEndpointTest {
  private val server = new EmbeddedThriftServer(
    twitterServer = new DoEverythingThriftTwitterServer(TestClientRequestHeaderKey)
  )

  private val muxCtxtsFilter = muxContextsFilter

  private val reqRepServicePerEndpoint: DoEverything.ReqRepServicePerEndpoint =
    ThriftMux.client
      .filtered(muxCtxtsFilter.toFilter)
      .configured(Stats(NullStatsReceiver))
      .withClientId(TestClientId)
      .reqRepServicePerEndpoint[DoEverything.ReqRepServicePerEndpoint](
        PortUtils.loopbackAddressForPort(server.thriftPort),
        "client123")

  /* test that copy works for client */
  private val filteredReqRepServicePerEndpoint: DoEverything.ReqRepServicePerEndpoint =
    reqRepServicePerEndpoint
      .withUppercase(
        uppercase = specificMethodLoggingFilter.andThen(reqRepServicePerEndpoint.uppercase))
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

  test("uppercase") {
    val request: Request[Uppercase.Args] =
      Request(
        headers = Map(
          TestClientRequestHeaderKey -> Seq(Buf.Utf8("bar")),
          "com.twitter.client123.another.header" -> Seq(Buf.Utf8("foo")),
          "com.twitter.finagle.Retries" -> Seq(Buf.Utf8( "5"))), // set a header that finagle already broadcast to ensure that we collect and don't blow up.
        args = Uppercase.Args("Hi"))
    val f: Future[Response[Uppercase.SuccessType]] =
      reqRepServicePerEndpoint.uppercase(request)
    val response = await(f)
    response.value should equal("HI")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.uppercase").head
    decodeBuf(buf) should equal("response")

    muxCtxtsFilter.contexts.isEmpty should be(false)
    muxCtxtsFilter.contexts.size should be(1)
    val (keyBuf, valBuf) = muxCtxtsFilter.contexts.head
    decodeBuf(keyBuf) should equal("com.twitter.doeverything.thriftscala.doeverything.uppercase")
    decodeBuf(valBuf) should equal("response")
  }

  test("filtered uppercase") {
    val request: Request[Uppercase.Args] =
      Request(Uppercase.Args("Hi"))
        .setHeader(TestClientRequestHeaderKey, "bar")
        .setHeader("com.twitter.finagle.Retries", "5") // set a header that finagle already broadcast to ensure that we collect and don't blow up.
    val f: Future[Response[Uppercase.SuccessType]] =
      filteredReqRepServicePerEndpoint.uppercase(request)
    val response = await(f)
    response.value should equal("HI")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.uppercase").head
    decodeBuf(buf) should equal("response")
  }

  test("echo") {
    val request: Request[Echo.Args] =
      Request(Echo.Args("Hello, World."))
        .setHeader(TestClientRequestHeaderKey, "bar")
    val f: Future[Response[Echo.SuccessType]] =
      reqRepServicePerEndpoint.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.echo").head
    decodeBuf(buf) should equal("response")
  }

  test("filtered echo") {
    val request: Request[Echo.Args] =
      Request(
        headers = Map(
          TestClientRequestHeaderKey -> Seq(Buf.Utf8("bar")),
          "com.twitter.client123.another.header" -> Seq(Buf.Utf8("foo"))),
        args = Echo.Args("Hello, World."))
    val f: Future[Response[Echo.SuccessType]] =
      filteredReqRepServicePerEndpoint.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    response.headers.toMap.size should be(1)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.echo").head
    decodeBuf(buf) should equal("response")
  }

  test("magicNum[Future]") {
    // use ServicePerEndpoint which has the 'muxCtxtsFilter' ThriftMux client filter
    val reqRepMethodPerEndpoint: DoEverything[Future] =
      ThriftMux.client
        .reqRepMethodPerEndpoint(reqRepServicePerEndpoint)

    val f: Future[String] =
      reqRepMethodPerEndpoint.magicNum()
    val response = await(f)
    response should equal("42")

    muxCtxtsFilter.contexts.isEmpty should be(false)
    muxCtxtsFilter.contexts.size should be(1)
    val (keyBuf, valBuf) = muxCtxtsFilter.contexts.head
    decodeBuf(keyBuf) should equal("com.twitter.doeverything.thriftscala.doeverything.magicNum")
    decodeBuf(valBuf) should equal("response")
  }

  test("methodPerEndpoint magicNum") {
    val methodPerEndpoint: DoEverything.MethodPerEndpoint =
      ThriftMux.client
        .filtered(muxCtxtsFilter.toFilter)
        .newIface[DoEverything.MethodPerEndpoint](
          PortUtils.loopbackAddressForPort(server.thriftPort),
          "client123")

    val f: Future[MagicNum.SuccessType] =
      methodPerEndpoint.magicNum()
    val response = await(f)
    response should equal("42")

    muxCtxtsFilter.contexts.isEmpty should be(false)
    muxCtxtsFilter.contexts.size should be(1)
    val (keyBuf, valBuf) = muxCtxtsFilter.contexts.head
    decodeBuf(keyBuf) should equal("com.twitter.doeverything.thriftscala.doeverything.magicNum")
    decodeBuf(valBuf) should equal("response")
  }
}
