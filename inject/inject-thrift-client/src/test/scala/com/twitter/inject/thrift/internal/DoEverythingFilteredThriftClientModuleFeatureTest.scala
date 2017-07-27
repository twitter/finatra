package com.twitter.inject.thrift.internal

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer, HttpTest}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.greeter.thriftscala.Greeter.{Bye, Hi}
import com.twitter.greeter.thriftscala.{Greeter, InvalidOperation}
import com.twitter.inject.Test
import com.twitter.inject.thrift.filtered_integration.http_server.{
  GreeterHttpController,
  HiLoggingThriftClientFilter
}
import com.twitter.inject.thrift.filtered_integration.thrift_server.GreeterThriftServer
import com.twitter.inject.thrift.filters.ThriftClientFilterBuilder
import com.twitter.inject.thrift.modules.{FilteredThriftClientModule, ThriftClientIdModule}
import com.twitter.util._
import scala.util.control.NonFatal

class DoEverythingFilteredThriftClientModuleFeatureTest extends Test with HttpTest {

  val thriftServer = new EmbeddedThriftServer(twitterServer = new GreeterThriftServer)

  val httpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val modules = Seq(ThriftClientIdModule, GreeterThriftClientModule2)

      override def configureHttp(router: HttpRouter) {
        router.filter[CommonFilters].add[GreeterHttpController]
      }
    },
    args = Seq(
      "-thrift.clientId=greeter-http-service",
      resolverMap("greeter-thrift-service" -> thriftServer.thriftHostAndPort)
    )
  )

  override def afterAll() {
    super.afterAll()
    httpServer.close()
    thriftServer.close()
  }

  override protected def afterEach(): Unit = {
    httpServer.clearStats()
    thriftServer.clearStats()
    super.afterEach()
  }

  test("GreeterHttpServer#Say hi") {
    httpServer.httpGet(path = "/hi?name=Bob", andExpect = Ok, withBody = "Hi Bob")

    // per-method -- all the requests in this test were to the same method
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/invocations", 1)
    /* assert counters added by ThriftServiceIface#statsFilter */
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/requests", 4)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/success", 2)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/hi/failures", 2)
    /* assert latency stat exists */
    httpServer.getStat("clnt/greeter-thrift-client/Greeter/hi/latency_ms") should not be Seq()
  }

  test("GreeterHttpServer#Say bye") {
    httpServer.httpGet(
      path = "/bye?name=Bob&age=18",
      andExpect = Ok,
      withBody = "Bye Bob of 18 years!"
    )

    // per-method -- all the requests in this test were to the same method
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/invocations", 1)
    /* assert counters added by StatsFilter */
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/requests", 3)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/success", 1)
    httpServer.assertCounter("clnt/greeter-thrift-client/Greeter/bye/failures", 2)
    /* assert latency stat exists */
    httpServer.getStat("clnt/greeter-thrift-client/Greeter/bye/latency_ms") should not be Seq()
    httpServer.getStat("clnt/greeter-thrift-client/Greeter/bye/request_latency_ms") should not be Seq()
  }
}

object GreeterThriftClientModule2
    extends FilteredThriftClientModule[Greeter[Future], Greeter.ServiceIface] {

  override val label = "greeter-thrift-client"
  override val dest = "flag!greeter-thrift-service"
  override val sessionAcquisitionTimeout = 1.minute.toDuration

  override def filterServiceIface(
    serviceIface: Greeter.ServiceIface,
    filter: ThriftClientFilterBuilder
  ) = {

    serviceIface.copy(
      hi = filter
        .method(Hi)
        .withAgnosticFilter(new RequestLoggingThriftClientFilter())
        .withMethodLatency
        .withConstantRetry(
          shouldRetry = {
            case (_, Throw(InvalidOperation(_))) => true
            case (_, Return(success)) => success == "ERROR"
            case (_, Throw(NonFatal(_))) => true
          },
          start = 50.millis,
          retries = 3
        )
        .withRequestTimeout(1.minute)
        .filtered(new HiLoggingThriftClientFilter)
        .andThen(serviceIface.hi),
      bye = filter
        .method(Bye)
        .withAgnosticFilter[RequestLoggingThriftClientFilter]
        .withMethodLatency
        .withExponentialRetry(
          shouldRetryResponse = PossiblyRetryableExceptions,
          start = 50.millis,
          multiplier = 2,
          retries = 3
        )
        .withRequestLatency
        .withRequestTimeout(1.minute)
        .andThen(serviceIface.bye)
    )
  }
}
