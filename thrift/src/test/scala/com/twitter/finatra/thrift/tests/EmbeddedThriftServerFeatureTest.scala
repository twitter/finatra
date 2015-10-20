package com.twitter.finatra.thrift.tests

import com.twitter.converter.thriftscala.Converter
import com.twitter.converter.thriftscala.Converter.Uppercase
import com.twitter.finagle.{Service, SimpleFilter, TimeoutException}
import com.twitter.finatra.logging.filter.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.thrift.codegen.MethodFilters
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.thriftscala.ClientErrorCause.RequestTimeout
import com.twitter.finatra.thrift.thriftscala.ServerErrorCause.InternalServerError
import com.twitter.finatra.thrift.thriftscala.{ClientError, NoClientIdError, ServerError, UnknownClientIdError}
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftRequest, ThriftRouter, ThriftServer}
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.{Logging, Test}
import com.twitter.util.{Await, Future, NonFatal}

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
}

class ConverterServer extends ThriftServer {
  override val modules = Seq(ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter[ThriftRequest, Any]]
      .filter[TraceIdMDCFilter[ThriftRequest, Any]]
      .filter(classOf[ThriftMDCFilter])
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[ConverterImpl](FilteredConverter.create)
  }
}

class ConverterImpl extends Converter[Future] {
  override def uppercase(msg: String): Future[String] = {
    if (msg == "fail")
      Future.exception(new Exception("oops"))
    else
      Future.value(msg.toUpperCase)
  }
}

object FilteredConverter {
  def create(filters: MethodFilters, underlying: Converter[Future]) = {
    new Converter[Future] {
      def uppercase(msg: String) = filters.create(Uppercase)(Service.mk(underlying.uppercase))(msg)
    }
  }
}

class ExceptionTranslationFilter
  extends SimpleFilter[ThriftRequest, Any]
  with Logging {

  override def apply(request: ThriftRequest, service: Service[ThriftRequest, Any]): Future[Any] = {
    service(request).rescue {
      case e: TimeoutException =>
        Future.exception(
          ClientError(RequestTimeout, e.getMessage))
      case e: ClientError =>
        Future.exception(e)
      case e: UnknownClientIdError =>
        Future.exception(e)
      case e: NoClientIdError =>
        Future.exception(e)
      case NonFatal(e) =>
        error("Unhandled exception", e)
        Future.exception(
          ServerError(InternalServerError, e.getMessage))
    }
  }
}
