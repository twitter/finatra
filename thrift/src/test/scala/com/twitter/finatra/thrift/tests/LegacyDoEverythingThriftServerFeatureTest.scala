package com.twitter.finatra.thrift.tests

import com.twitter.conversions.DurationOps._
import com.twitter.doeverything.thriftscala.Answer
import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.doeverything.thriftscala.Question
import com.twitter.finagle.tracing.Flags
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.tracing.TraceId
import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.LegacyDoEverythingThriftServer
import com.twitter.finatra.thrift.tests.doeverything.controllers.LegacyDoEverythingThriftController
import com.twitter.inject.server.FeatureTest
import com.twitter.io.Buf
import com.twitter.scrooge
import com.twitter.util.Await
import com.twitter.util.Future
import org.apache.thrift.TApplicationException

@deprecated(
  "These tests exist to ensure legacy functionality still operates. Do not use them for guidance",
  "2018-12-20")
class LegacyDoEverythingThriftServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(
    twitterServer = new LegacyDoEverythingThriftServer,
    disableTestLogging = true,
    flags = Map("magicNum" -> "57")
  )

  /* Method-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id1 */
  val client123: DoEverything.MethodPerEndpoint =
    server.thriftClient[DoEverything.MethodPerEndpoint](clientId = "client123")
  /* Service-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id2 */
  val servicePerEndpoint123: DoEverything.ServicePerEndpoint =
    server.servicePerEndpoint[DoEverything.ServicePerEndpoint](clientId = "client123")

  /* Another Method-Per-Endpoint type wrapping a Service-per-endpoint: https://twitter.github.io/scrooge/Finagle.html#id1 */
  val yetAnotherMethodPerEndpointClient123: DoEverything.MethodPerEndpoint =
    server.methodPerEndpoint[DoEverything.ServicePerEndpoint, DoEverything.MethodPerEndpoint](
      servicePerEndpoint123
    )
  /* Req/Rep Service-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id3 */
  val reqRepServicePerEndpoint123: DoEverything.ReqRepServicePerEndpoint =
    server.servicePerEndpoint[DoEverything.ReqRepServicePerEndpoint](clientId = "client123")

  test("success") {
    await(client123.uppercase("Hi")) should equal("HI")
    await(yetAnotherMethodPerEndpointClient123.uppercase("Hi")) should equal("HI")

    val filter = new Filter[
      DoEverything.Uppercase.Args,
      DoEverything.Uppercase.SuccessType,
      DoEverything.Uppercase.Args,
      DoEverything.Uppercase.SuccessType
    ] {
      override def apply(
        request: DoEverything.Uppercase.Args,
        service: Service[DoEverything.Uppercase.Args, String]
      ): Future[String] = {
        if (request.msg == "hello") {
          service(DoEverything.Uppercase.Args("goodbye"))
        } else service(request)
      }
    }
    val service = filter.andThen(servicePerEndpoint123.uppercase)
    await(service(DoEverything.Uppercase.Args("hello"))) should equal("GOODBYE")

    val filter2 = new Filter[scrooge.Request[DoEverything.Uppercase.Args], scrooge.Response[
      DoEverything.Uppercase.SuccessType
    ], scrooge.Request[DoEverything.Uppercase.Args], scrooge.Response[
      DoEverything.Uppercase.SuccessType
    ]] {
      override def apply(
        request: scrooge.Request[DoEverything.Uppercase.Args],
        service: Service[scrooge.Request[DoEverything.Uppercase.Args], scrooge.Response[
          DoEverything.Uppercase.SuccessType
        ]]
      ): Future[scrooge.Response[DoEverything.Uppercase.SuccessType]] = {
        val filteredRequest: scrooge.Request[DoEverything.Uppercase.Args] =
          scrooge.Request(Map("com.twitter.test.header" -> Seq(Buf.Utf8("foo"))), request.args)
        service(filteredRequest)
      }
    }
    val service2 = filter2.andThen(reqRepServicePerEndpoint123.uppercase)
    await(service2(scrooge.Request(DoEverything.Uppercase.Args("hello")))).value should equal(
      "HELLO"
    )
  }

  test("failure") {
    val e = assertFailedFuture[Exception] {
      client123.uppercase("fail")
    }
    e.getMessage should include("oops")
  }

  test("magicNum") {
    await(client123.magicNum()) should equal("57")
  }

  // echo method doesn't define throws ClientError Exception
  // we should receive TApplicationException
  test("ClientError throw back") {
    assertFailedFuture[TApplicationException] {
      client123.echo("clientError")
    }
  }

  // should be caught by FinatraThriftExceptionMapper
  test("ThriftException#ClientError mapping") {
    val e = assertFailedFuture[TApplicationException] {
      client123.echo2("clientError")
    }
    e.getMessage should include("client error")
  }

  test("ThriftException#UnknownClientIdError mapping") {
    val e = assertFailedFuture[TApplicationException] {
      client123.echo2("unknownClientIdError")
    }
    e.getMessage should include("unknown client id error")
  }

  test("ThriftException#RequestException mapping") {
    assertFailedFuture[TApplicationException] {
      client123.echo2("requestException")
    }
  }

  test("ThriftException#TimeoutException mapping") {
    assertFailedFuture[TApplicationException] {
      client123.echo2("timeoutException")
    }
  }

  // should be caught by ReqRepBarExceptionMapper
  test("BarException mapping") {
    await(client123.echo2("barException")) should equal("BarException caught")
  }
  // should be caught by ReqRepFooExceptionMapper
  test("FooException mapping") {
    await(client123.echo2("fooException")) should equal("FooException caught")
  }

  test("ThriftException#UnhandledSourcedException mapping") {
    assertFailedFuture[TApplicationException] {
      client123.echo2("unhandledSourcedException")
    }
  }

  test("ThriftException#UnhandledException mapping") {
    assertFailedFuture[TApplicationException] {
      client123.echo2("unhandledException")
    }
  }

  // should be caught by framework root exception mapper - ThrowableExceptionMapper
  test("ThriftException#UnhandledThrowable mapping") {
    assertFailedFuture[TApplicationException] {
      client123.echo2("unhandledThrowable")
    }
  }

  test("more than 22 args") {
    await(
      client123.moreThanTwentyTwoArgs(
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "eleven",
        "twelve",
        "thirteen",
        "fourteen",
        "fifteen",
        "sixteen",
        "seventeen",
        "eighteen",
        "nineteen",
        "twenty",
        "twentyone",
        "twentytwo",
        "twentythree"
      )
    ) should equal("handled")
  }

  test("ask") {
    val question = Question("What is the meaning of life?")
    await(client123.ask(question)) should equal(
      Answer("The answer to the question: `What is the meaning of life?` is 42.")
    )
  }

  test("ask fail") {
    val question = Question("fail")
    await(client123.ask(question)) should equal(Answer("DoEverythingException caught"))
  }

  test("MDC filtering with trace sampling undecided") {
    val traceId = Trace.nextId
    val response = await {
      Trace.letId(traceId) {
        client123.uppercase("Hi")
      }
    }

    response should equal("HI")

    val MDC = server.injector.instance[LegacyDoEverythingThriftController].getStoredMDC
    MDC should not be None
    MDC.get.size should equal(5)

    MDC.get("method") should not be null
    MDC.get("method") should be("uppercase")

    MDC.get("clientId") should not be null
    MDC.get("clientId") should be("client123")

    MDC.get("traceId") should not be null
    MDC.get("traceId") should be(traceId.traceId.toString())

    MDC.get("traceSpanId") should not be null
    // A new span Id would be created for the client call.
    MDC.get("traceSpanId") should not be (traceId.traceId.toString)

    MDC.get("traceSampled") should not be null
    MDC.get("traceSampled") should be("false")
  }

  test("MDC filtering with sampled set to true") {
    val tempTraceId = Trace.nextId
    val traceId = TraceId(None, None, tempTraceId.spanId, None, Flags().setDebug, None)

    val response = await {
      Trace.letId(traceId) {
        client123.uppercase("Hi")
      }
    }

    response should equal("HI")

    val MDC = server.injector.instance[LegacyDoEverythingThriftController].getStoredMDC
    MDC should not be None
    MDC.get.size should equal(5)

    MDC.get("method") should not be null
    MDC.get("method") should be("uppercase")

    MDC.get("clientId") should not be null
    MDC.get("clientId") should be("client123")

    MDC.get("traceId") should not be null
    MDC.get("traceId") should be(traceId.traceId.toString())

    MDC.get("traceSpanId") should not be null
    // A new span Id would be created for the client call.
    MDC.get("traceSpanId") should not be (traceId.traceId.toString)

    MDC.get("traceSampled") should not be null
    MDC.get("traceSampled") should be("true")
  }

  test("Basic server stats") {
    await(client123.uppercase("Hi")) should equal("HI")
    server.inMemoryStats.counters.assert("srv/thrift/sent_bytes")(_ > 0)
    server.inMemoryStats.counters.assert("srv/thrift/received_bytes")(_ > 0)
    server.inMemoryStats.counters.assert("srv/thrift/requests", 1)
    server.inMemoryStats.counters.assert("srv/thrift/success", 1)
  }

  test("Per-method stats scope") {
    val question = Question("fail")
    await(client123.ask(question)) should equal(Answer("DoEverythingException caught"))
    server.inMemoryStats.counters.assert("per_method_stats/ask/success", 1)
    server.inMemoryStats.counters.get("per_method_stats/ask/failures") should be(None)
  }

  test("Per-endpoint stats scope") {
    val question = Question("fail")
    await(client123.ask(question)) should equal(Answer("DoEverythingException caught"))
    server.inMemoryStats.counters.assert("srv/thrift/ask/requests", 1)
    server.inMemoryStats.counters.assert("srv/thrift/ask/success", 1)
    server.inMemoryStats.counters.get("srv/thrift/ask/failures") should be(None)
  }

  private def await[T](f: Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  override protected def beforeEach(): Unit = {
    server.inMemoryStatsReceiver.clear()
  }
}
