package com.twitter.finatra.thrift.tests

import com.twitter.conversions.time._
import com.twitter.doeverything.thriftscala.{Answer, DoEverything, Question}
import com.twitter.finagle.http.Status
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftServer
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoEverythingThriftController
import com.twitter.finatra.thrift.thriftscala.{ClientError, NoClientIdError, ServerError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.io.Buf
import com.twitter.scrooge
import com.twitter.util.{Await, Future}
import org.apache.thrift.TApplicationException
import scala.util.parsing.json.JSON

class DoEverythingThriftServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(
    twitterServer = new DoEverythingThriftServer,
    disableTestLogging = true,
    flags = Map("magicNum" -> "57")
  )

  /* Higher-kinded interface type */
  val client123: DoEverything[Future] = server.thriftClient[DoEverything[Future]](clientId = "client123")
  /* Method-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id1 */
  val methodPerEndpointClient123: DoEverything.MethodPerEndpoint =
    server.thriftClient[DoEverything.MethodPerEndpoint](clientId = "client123")
  /* Service-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id2 */
  val servicePerEndpoint123: DoEverything.ServicePerEndpoint =
    server.servicePerEndpoint[DoEverything.ServicePerEndpoint](clientId = "client123")
  /* Higher-kinded interface type wrapping a Service-per-endpoint: https://twitter.github.io/scrooge/Finagle.html#id1 */
  val anotherMethodPerEndpointClient123: DoEverything[Future] =
    server.thriftClient[DoEverything.ServicePerEndpoint, DoEverything[Future]](
      servicePerEndpoint123
    )
  /* Another Method-Per-Endpoint type wrapping a Service-per-endpoint: https://twitter.github.io/scrooge/Finagle.html#id1 */
  val yetAnotherMethodPerEndpointClient123: DoEverything.MethodPerEndpoint =
    server.methodPerEndpoint[DoEverything.ServicePerEndpoint, DoEverything.MethodPerEndpoint](
      servicePerEndpoint123
    )
  /* Req/Rep Service-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id3 */
  val reqRepServicePerEndpoint123: DoEverything.ReqRepServicePerEndpoint =
    server.servicePerEndpoint[DoEverything.ReqRepServicePerEndpoint](clientId = "client123")

  override protected def afterAll(): Unit = {
    Await.all(
      Seq(
        client123.asClosable.close(),
        methodPerEndpointClient123.asClosable.close(),
        servicePerEndpoint123.asClosable.close(),
        anotherMethodPerEndpointClient123.asClosable.close(),
        yetAnotherMethodPerEndpointClient123.asClosable.close(),
        reqRepServicePerEndpoint123.asClosable.close()
      ),
      2.seconds
    )
    super.afterAll()
  }

  test("success") {
    await(client123.uppercase("Hi")) should equal("HI")
    await(methodPerEndpointClient123.uppercase("Hi")) should equal("HI")
    await(anotherMethodPerEndpointClient123.uppercase("Hi")) should equal("HI")
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

  test("blacklist") {
    val notWhitelistClient =
      server.thriftClient[DoEverything[Future]](clientId = "not_on_whitelist")
    assertFailedFuture[UnknownClientIdError] {
      notWhitelistClient.echo("Hi")
    }
  }

  test("no client id") {
    val noClientIdClient = server.thriftClient[DoEverything[Future]]()
    assertFailedFuture[NoClientIdError] {
      noClientIdClient.echo("Hi")
    }
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
    val e = assertFailedFuture[ClientError] {
      client123.echo2("clientError")
    }
    e.getMessage should include("client error")
  }

  test("ThriftException#UnknownClientIdError mapping") {
    val e = assertFailedFuture[UnknownClientIdError] {
      client123.echo2("unknownClientIdError")
    }
    e.getMessage should include("unknown client id error")
  }

  test("ThriftException#RequestException mapping") {
    assertFailedFuture[ServerError] {
      client123.echo2("requestException")
    }
  }

  test("ThriftException#TimeoutException mapping") {
    assertFailedFuture[ClientError] {
      client123.echo2("timeoutException")
    }
  }

  // should be caught by BarExceptionMapper
  test("BarException mapping") {
    await(client123.echo2("barException")) should equal("BarException caught")
  }
  // should be caught by FooExceptionMapper
  test("FooException mapping") {
    await(client123.echo2("fooException")) should equal("FooException caught")
  }

  test("ThriftException#UnhandledSourcedException mapping") {
    assertFailedFuture[ServerError] {
      client123.echo2("unhandledSourcedException")
    }
  }

  test("ThriftException#UnhandledException mapping") {
    assertFailedFuture[ServerError] {
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

  test("MDC filtering") {
    val traceId = Trace.nextId
    val response = await {
      Trace.letId(traceId) {
        client123.uppercase("Hi")
      }
    }

    response should equal("HI")

    val MDC = server.injector.instance[DoEverythingThriftController].getStoredMDC
    MDC should not be None
    MDC.get.size should equal(3)

    MDC.get("method") should not be null
    MDC.get("method") should be("uppercase")

    MDC.get("clientId") should not be null
    MDC.get("clientId") should be("client123")

    MDC.get("traceId") should not be null
    MDC.get("traceId") should be(traceId.traceId.toString())
  }

  test("GET /admin/registry.json") {
    val response = server.httpGetAdmin(
      "/admin/registry.json",
      andExpect = Status.Ok)

    val json: Map[String, Any] =
      JSON.parseFull(response.contentString).get.asInstanceOf[Map[String, Any]]

    val registry = json("registry").asInstanceOf[Map[String, Any]]
    registry.contains("library") should be(true)
    registry("library").asInstanceOf[Map[String, String]].contains("finatra") should be(true)

    val finatra = registry("library")
      .asInstanceOf[Map[String, Any]]("finatra")
      .asInstanceOf[Map[String, Any]]

    finatra.contains("thrift") should be(true)
    val thrift = finatra("thrift").asInstanceOf[Map[String, Any]]
    thrift.contains("filters") should be(true)
    thrift.contains("methods") should be(true)

    val methods = thrift("methods").asInstanceOf[Map[String, Any]]
    methods.size should be > 0

    methods.foreach { case (_, data) =>
      data.isInstanceOf[Map[_, _]] should be(true)
      val methodJsonInformation = data.asInstanceOf[Map[String, Any]]
      methodJsonInformation.contains("service_name") should be(true)
      methodJsonInformation.contains("class") should be(true)
    }
  }

  private def await[T](f: Future[T]): T = {
    Await.result(f, 2.seconds)
  }
}
