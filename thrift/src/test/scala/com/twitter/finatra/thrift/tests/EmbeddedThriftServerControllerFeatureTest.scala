package com.twitter.finatra.thrift.tests

import com.twitter.converter.thriftscala.Converter
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.filters.ExceptionTranslationFilter
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class EmbeddedThriftServerControllerIntegrationTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(new ConverterControllerServer)

  val client123 = server.thriftClient[Converter[Future]](clientId = "client123")

  "success" in {
    Await.result(client123.uppercase("Hi")) should equal("HI")
  }

  "failure" in {
    val e = assertFailedFuture[Exception] {
      client123.uppercase("fail")
    }
    e.getMessage should include("oops")
  }

  "blacklist" in {
    val notWhitelistClient = server.thriftClient[Converter[Future]](clientId = "not_on_whitelist")
    assertFailedFuture[UnknownClientIdError] {
      notWhitelistClient.uppercase("Hi")
    }
  }

  "no client id" in {
    val noClientIdClient = server.thriftClient[Converter[Future]]()
    assertFailedFuture[NoClientIdError] {
      noClientIdClient.uppercase("Hi")
    }
  }

  "more than 22 args" in {
    Await.result(
      client123.moreThanTwentyTwoArgs("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "twentyone", "twentytwo", "twentythree")
    ) should equal("foo")
  }
}

class ConverterControllerServer extends ThriftServer {
  override val modules = Seq(ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[ConverterController]
  }
}

class ConverterController
  extends Controller
  with Converter.BaseServiceIface {
  import com.twitter.converter.thriftscala.Converter._

  val uppercase = handle(Uppercase) { args: Uppercase.Args =>
    if (args.msg == "fail")
      Future.exception(new Exception("oops"))
    else
      Future.value(args.msg.toUpperCase)
  }

  val moreThanTwentyTwoArgs = handle(MoreThanTwentyTwoArgs) { args : MoreThanTwentyTwoArgs.Args =>
    Future.value("foo")
  }
}
