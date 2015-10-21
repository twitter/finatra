package com.twitter.inject.thrift.internal

import com.twitter.finagle.Thrift
import com.twitter.finagle.http.Status._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.greeter.thriftscala.{InvalidOperation, Greeter}
import com.twitter.greeter.thriftscala.Greeter.{Bye, Hi}
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.inject.thrift.filtered_integration.http_server.{HiLoggingThriftClientFilter, GreeterHttpController}
import com.twitter.inject.thrift.filtered_integration.thrift_server.GreeterThriftServer
import com.twitter.inject.thrift.{FilterBuilder, FilteredThriftClientModule, ThriftClientIdModule}
import com.twitter.scrooge.ThriftResponse
import com.twitter.util._

class DoEverythingFilteredThriftClientModuleFeatureTest extends HttpTest {

  val thriftServer = new EmbeddedTwitterServer(
    twitterServer = new GreeterThriftServer)

  val httpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val modules = Seq(
        ThriftClientIdModule,
        GreeterThriftClientModule2)

      override def configureHttp(router: HttpRouter) {
        router.
          filter[CommonFilters].
          add[GreeterHttpController]
      }
    },
    extraArgs = Seq(
      "-thrift.clientId=greeter-http-service",
      resolverMap("greeter-thrift-service" -> thriftServer.thriftHostAndPort)))

  override def afterAll() {
    super.afterAll()
    httpServer.close()
    thriftServer.close()
  }

  "GreeterHttpServer" should {
    "Say hi" in {
      httpServer.httpGet(
        path = "/hi?name=Bob",
        andExpect = Ok,
        withBody = "Hi Bob")
    }

    "Say bye" in {
      httpServer.httpGet(
        path = "/bye?name=Bob&age=18",
        andExpect = Ok,
        withBody = "Bye Bob of 18 years!")
    }
  }
}

object GreeterThriftClientModule2
  extends FilteredThriftClientModule[Greeter[Future], Greeter.ServiceIface] {

  override val label = "greeter-thrift-client"
  override val dest = "flag!greeter-thrift-service"
  override val connectTimeout = 1.minute.toDuration

  override def createFilteredClient(
    serviceIface: Greeter.ServiceIface,
    filterBuilder: FilterBuilder): Greeter[Future] = {

    Thrift.newMethodIface(serviceIface.copy(
      hi = filterBuilder.method(Hi)
        .constantRetry(
          requestTimeout = 1.minute,
          shouldRetry = {
            case (_, Return(Hi.Result(_, Some(e: InvalidOperation)))) => true
            case (_, Return(Hi.Result(Some(success), _))) => success == "ERROR"
            case (_, Throw(NonFatal(_))) => true
          },
          start = 50.millis,
          retries = 3)
        .globalFilter(new RequestLoggingThriftClientFilter)
        .filter(new HiLoggingThriftClientFilter)
        .andThen(serviceIface.hi),
      bye = filterBuilder.method(Bye)
        .globalFilter[RequestLoggingThriftClientFilter]
        .exponentialRetry(
          shouldRetryResponse = NonFatalExceptions,
          requestTimeout = 1.minute,
          start = 50.millis,
          multiplier = 2,
          retries = 3)
        .andThen(serviceIface.bye)))
  }
}
