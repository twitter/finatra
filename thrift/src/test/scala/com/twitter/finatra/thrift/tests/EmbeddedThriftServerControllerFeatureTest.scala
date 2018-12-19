package com.twitter.finatra.thrift.tests

import com.twitter.conversions.DurationOps._
import com.twitter.converter.thriftscala.Converter
import com.twitter.converter.thriftscala.Converter.Uppercase
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.exceptions.FinatraThriftExceptionMapper
import com.twitter.finatra.thrift.filters.{
  AccessLoggingFilter,
  ClientIdAcceptlistFilter,
  ExceptionMappingFilter,
  StatsFilter
}
import com.twitter.finatra.thrift.modules.ClientIdAcceptlistModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.EmbeddedThriftServerControllerFeatureTest._
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.io.Buf
import com.twitter.scrooge
import com.twitter.util.{Await, Future}

object EmbeddedThriftServerControllerFeatureTest {

  class ConverterControllerServer extends ThriftServer {
    override val modules = Seq(new ClientIdAcceptlistModule("/clients.yml"))

    override def configureThrift(router: ThriftRouter): Unit = {
      router
        .filter(classOf[AccessLoggingFilter])
        .filter[StatsFilter]
        .filter[ExceptionMappingFilter]
        .filter[ClientIdAcceptlistFilter]
        .exceptionMapper[FinatraThriftExceptionMapper]
        .add[ConverterController]
    }
  }

  class ConverterController extends Controller with Converter.BaseServiceIface {
    import com.twitter.converter.thriftscala.Converter._

    val uppercase = handle(Uppercase) { args: Uppercase.Args =>
      if (args.msg == "fail")
        Future.exception(new Exception("oops"))
      else
        Future.value(args.msg.toUpperCase)
    }

    val moreThanTwentyTwoArgs = handle(MoreThanTwentyTwoArgs) { _: MoreThanTwentyTwoArgs.Args =>
      Future.value("foo")
    }
  }
}

class EmbeddedThriftServerControllerFeatureTest extends FeatureTest {
  override val server =
    new EmbeddedThriftServer(new ConverterControllerServer, disableTestLogging = true)

  /* Higher-kinded interface type */
  val client123: Converter[Future] =
    server.thriftClient[Converter[Future]](clientId = "client123")
  /* Method-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id1 */
  val methodPerEndpointClient123: Converter.MethodPerEndpoint =
    server.thriftClient[Converter.MethodPerEndpoint](clientId = "client123")
  /* Service-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id2 */
  val servicePerEndpoint123: Converter.ServicePerEndpoint =
    server.servicePerEndpoint[Converter.ServicePerEndpoint](clientId = "client123")
  /* Req/Rep Service-Per-Endpoint type: https://twitter.github.io/scrooge/Finagle.html#id3 */
  val reqRepServicePerEndpoint123: Converter.ReqRepServicePerEndpoint =
    server.servicePerEndpoint[Converter.ReqRepServicePerEndpoint](clientId = "client123")

  override protected def afterAll(): Unit = {
    Await.all(
      Seq(
        client123.asClosable.close(),
        methodPerEndpointClient123.asClosable.close(),
        servicePerEndpoint123.asClosable.close(),
        reqRepServicePerEndpoint123.asClosable.close()
      ),
      2.seconds
    )
    super.afterAll()
  }

  test("success") {
    await(client123.uppercase("Hi")) should equal("HI")
    await(methodPerEndpointClient123.uppercase("Hi")) should equal("HI")

    val filter = new Filter[
      Converter.Uppercase.Args,
      Converter.Uppercase.SuccessType,
      Converter.Uppercase.Args,
      Converter.Uppercase.SuccessType
    ] {
      override def apply(
        request: Uppercase.Args,
        service: Service[Uppercase.Args, String]
      ): Future[String] = {
        if (request.msg == "hello") {
          service(Converter.Uppercase.Args("goodbye"))
        } else service(request)
      }
    }
    val service = filter.andThen(servicePerEndpoint123.uppercase)
    await(service(Converter.Uppercase.Args("hello"))) should equal("GOODBYE")

    val filter2 =
      new Filter[scrooge.Request[Converter.Uppercase.Args], scrooge.Response[
        Converter.Uppercase.SuccessType
      ], scrooge.Request[Converter.Uppercase.Args], scrooge.Response[
        Converter.Uppercase.SuccessType
      ]] {
        override def apply(
          request: scrooge.Request[Converter.Uppercase.Args],
          service: Service[scrooge.Request[Converter.Uppercase.Args], scrooge.Response[
            Converter.Uppercase.SuccessType
          ]]
        ): Future[scrooge.Response[Converter.Uppercase.SuccessType]] = {
          val filteredRequest: scrooge.Request[Converter.Uppercase.Args] =
            scrooge.Request(Map("com.twitter.test.header" -> Seq(Buf.Utf8("foo"))), request.args)
          service(filteredRequest)
        }
      }
    val service2 = filter2.andThen(reqRepServicePerEndpoint123.uppercase)
    await(service2(scrooge.Request(Converter.Uppercase.Args("hello")))).value should equal("HELLO")
  }

  test("failure") {
    val e = assertFailedFuture[Exception] {
      client123.uppercase("fail")
    }
    e.getMessage should include("oops")
  }

  test("blacklist") {
    val notWhitelistClient = server.thriftClient[Converter[Future]](clientId = "not_on_whitelist")
    assertFailedFuture[UnknownClientIdError] {
      notWhitelistClient.uppercase("Hi")
    }
  }

  test("no client id") {
    val noClientIdClient = server.thriftClient[Converter[Future]]()
    assertFailedFuture[NoClientIdError] {
      noClientIdClient.uppercase("Hi")
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
    ) should equal("foo")
  }
}
