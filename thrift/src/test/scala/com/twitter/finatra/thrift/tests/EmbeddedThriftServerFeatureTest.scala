package com.twitter.finatra.thrift.tests

import com.twitter.converter.thriftscala.Converter
import com.twitter.converter.thriftscala.Converter.Uppercase
import com.twitter.finagle.Service
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.codegen.MethodFilters
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.tests.doeverything.filters.ExceptionTranslationFilter
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class EmbeddedThriftServerIntegrationTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(new ConverterServer)

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

  // Methods with more than 22 args are not supported
  // Please use a com.twitter.finatra.thrift.Controller to implement methods with more than 22 args
  /*
  "more than 22 args" in {
    Await.result(
      client123.moreThanTwentyTwoArgs("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "twentyone", "twentytwo", "twentythree")
    ) should equal("foo")
  }
  */
}

// @deprecated("Thrift services should be defined as a Thrift controller", "2016-01-26")
class ConverterServer extends ThriftServer {
  override val modules = Seq(ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[ConverterImpl](FilteredConverter.create)
  }
}

// @deprecated("Thrift services should be defined as a Thrift controller", "2016-01-26")
class ConverterImpl extends Converter[Future] {
  override def uppercase(msg: String): Future[String] = {
    if (msg == "fail")
      Future.exception(new Exception("oops"))
    else
      Future.value(msg.toUpperCase)
  }

  override def moreThanTwentyTwoArgs(one: String, two: String, three: String, four: String, five: String, six: String, seven: String, eight: String, nine: String, ten: String, eleven: String, twelve: String, thirteen: String, fourteen: String, fifteen: String, sixteen: String, seventeen: String, eighteen: String, nineteen: String, twenty: String, twentyone: String, twentytwo: String, twentythree: String): Future[String] =
    Future.value("foo")
}

// @deprecated("Thrift services should be filtered with ThriftRouter#filter or ThriftRouter#typeAgnosticFilter", "2016-01-26")
object FilteredConverter {
  def create(filters: MethodFilters, underlying: Converter[Future]) = {
    new Converter[Future] {
      def uppercase(msg: String): Future[String] = filters.create(Uppercase)(Service.mk(underlying.uppercase))(msg)
      def moreThanTwentyTwoArgs(one: String, two: String, three: String, four: String, five: String, six: String, seven: String, eight: String, nine: String, ten: String, eleven: String, twelve: String, thirteen: String, fourteen: String, fifteen: String, sixteen: String, seventeen: String, eighteen: String, nineteen: String, twenty: String, twentyone: String, twentytwo: String, twentythree: String): Future[String] =
        // scala doesn't support functions with more than 22 args
        //filters.create(MoreThanTwentyTwoArgs)(Service.mk(underlying.moreThanTwentyTwoArgs))(one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve, thirteen, fourteen, fifteen, sixteen, seventeen, eighteen, nineteen, twenty, twentyone, twentytwo, twentythree)
        ???
    }
  }
}
