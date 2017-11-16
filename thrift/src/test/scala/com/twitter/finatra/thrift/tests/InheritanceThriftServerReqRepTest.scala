package com.twitter.finatra.thrift.tests

import com.twitter.conversions.time._
import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.{Filter, Service, ThriftMux}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.inheritance.InheritanceThriftTwitterServer
import com.twitter.inject.Test
import com.twitter.inject.server.PortUtils
import com.twitter.io.Buf
import com.twitter.scrooge.{Request, Response}
import com.twitter.serviceA.thriftscala.ServiceA.Echo
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.serviceB.thriftscala.ServiceB.Ping
import com.twitter.util.{Await, Future}
import java.nio.charset.{StandardCharsets => JChar}

class InheritanceThriftServerReqRepTest extends Test {
  private val server = new EmbeddedThriftServer(
    twitterServer = new InheritanceThriftTwitterServer
  )

  private def await[T](f: Future[T]): T = {
    Await.result(f, 5.seconds)
  }

  private val specificMethodLoggingFilter = new TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        def apply(
          request: Req,
          service: Service[Req, Rep]
        ): Future[Rep] = {
          info("METHOD CLIENT-SIDE FILTER")
          service(request)
        }
      }
    }
  }

  private val globalLoggingFilter = new TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        def apply(
          request: Req,
          service: Service[Req, Rep]
        ): Future[Rep] = {
          info("GLOBAL CLIENT-SIDE FILTER")
          service(request)
        }
      }
    }
  }

  private val reqRepServiceClient123: ServiceB.ReqRepServicePerEndpoint =
    ThriftMux.client
      .configured(Stats(NullStatsReceiver))
      .withClientId(ClientId("client123"))
      .reqRepServicePerEndpoint[ServiceB.ReqRepServicePerEndpoint](
        PortUtils.loopbackAddressForPort(server.thriftPort), "client123")

  /* test that copy works for client */
  val filteredReqRepServiceClient123: ServiceB.ReqRepServicePerEndpoint =
    reqRepServiceClient123
      .withPing(
        ping = specificMethodLoggingFilter.andThen(reqRepServiceClient123.ping))
      .withEcho(reqRepServiceClient123.echo)
    .filtered(globalLoggingFilter)

  override def beforeAll(): Unit = {
    server.start()
  }

  override def afterAll(): Unit = {
    server.close()
  }

  test("ping") {
    val request: Request[Ping.Args] =
      Request(Ping.Args())
        .setHeader("com.twitter.client123.foo", "bar")
        .setHeader("com.twitter.finagle.Retries", "5") // set a header that finagle already broadcast to ensure that we collect and don't blow up.
    val f: Future[Response[Ping.SuccessType]] =
      reqRepServiceClient123.ping(request)
    val response = await(f)
    response.value should equal("pong")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.ping").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }

  test("filtered ping") {
    val request: Request[Ping.Args] =
      Request(
        headers = Map(
          "com.twitter.client123.foo" -> Seq(Buf.Utf8("bar")),
          "com.twitter.finagle.Retries" ->  Seq(Buf.Utf8("5"))), // set a header that finagle already broadcast to ensure that we collect and don't blow up.
        args = Ping.Args())
    val f: Future[Response[Ping.SuccessType]] =
      filteredReqRepServiceClient123.ping(request)
    val response = await(f)
    response.value should equal("pong")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.ping").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }

  test("echo") {
    val request: Request[Echo.Args] =
      Request(
        headers = Map("com.twitter.client123.foo" -> Seq(Buf.Utf8("bar"))),
        args = Echo.Args("Hello, World."))
    val f: Future[Response[Echo.SuccessType]] =
      reqRepServiceClient123.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.echo").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }

  test("filtered echo") {
    val request: Request[Echo.Args] =
      Request(Echo.Args("Hello, World."))
        .setHeader("com.twitter.client123.foo", "bar")
    val f: Future[Response[Echo.SuccessType]] =
      filteredReqRepServiceClient123.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.echo").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }
}
