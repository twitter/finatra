package com.twitter.finatra.thrift.tests

import com.twitter.conversions.time._
import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.doeverything.thriftscala.DoEverything.{Echo, Uppercase}
import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.{Filter, Service, ThriftMux}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftTwitterServer
import com.twitter.inject.Test
import com.twitter.inject.server.PortUtils
import com.twitter.io.Buf
import com.twitter.scrooge.{Request, Response}
import com.twitter.util.{Await, Future}
import java.nio.charset.{StandardCharsets => JChar}

class DoEverythingThriftServerReqRepTest extends Test {
  private val server = new EmbeddedThriftServer(
    twitterServer = new DoEverythingThriftTwitterServer
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

  private val reqRepServiceClient123: DoEverything.ReqRepServicePerEndpoint =
    ThriftMux.client
      .configured(Stats(NullStatsReceiver))
      .withClientId(ClientId("client123"))
      .reqRepServicePerEndpoint[DoEverything.ReqRepServicePerEndpoint](
        PortUtils.loopbackAddressForPort(server.thriftPort), "client123")

  /* test that copy works for client */
  val filteredReqRepServiceClient123: DoEverything.ReqRepServicePerEndpoint =
    reqRepServiceClient123
      .withUppercase(
        uppercase = specificMethodLoggingFilter.andThen(reqRepServiceClient123.uppercase))
    .filtered(globalLoggingFilter)

  override def beforeAll(): Unit = {
    server.start()
  }

  override def afterAll(): Unit = {
    server.close()
  }

  test("uppercase") {
    val request: Request[Uppercase.Args] =
      Request(
        headers = Map(
          "com.twitter.client123.foo" -> Seq(Buf.Utf8("bar")),
          "com.twitter.finagle.Retries" -> Seq(Buf.Utf8( "5"))), // set a header that finagle already broadcast to ensure that we collect and don't blow up.
        args = Uppercase.Args("Hi"))
    val f: Future[Response[Uppercase.SuccessType]] =
      reqRepServiceClient123.uppercase(request)
    val response = await(f)
    response.value should equal("HI")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.uppercase").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }

  test("filtered uppercase") {
    val request: Request[Uppercase.Args] =
      Request(Uppercase.Args("Hi"))
        .setHeader("com.twitter.client123.foo", "bar")
        .setHeader("com.twitter.finagle.Retries", "5") // set a header that finagle already broadcast to ensure that we collect and don't blow up.
    val f: Future[Response[Uppercase.SuccessType]] =
      filteredReqRepServiceClient123.uppercase(request)
    val response = await(f)
    response.value should equal("HI")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.uppercase").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }

  test("echo") {
    val request: Request[Echo.Args] =
      Request(Echo.Args("Hello, World."))
        .setHeader("com.twitter.client123.foo", "bar")
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
      Request(
        headers = Map("com.twitter.client123.foo" -> Seq(Buf.Utf8("bar"))),
        args = Echo.Args("Hello, World."))
    val f: Future[Response[Echo.SuccessType]] =
      filteredReqRepServiceClient123.echo(request)
    val response = await(f)
    response.value should equal("Hello, World.")
    response.headers.isEmpty should be(false)
    val buf = response.headers("com.twitter.doeverything.thriftscala.doeverything.echo").head
    Buf.decodeString(buf, JChar.UTF_8) should equal("response")
  }
}
