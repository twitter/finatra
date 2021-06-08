package com.twitter.finatra.thrift.tests.inheritance

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Filter, ListeningServer, NullServer, Service, ThriftMux}
import com.twitter.finatra.thrift.tests.ReqRepServicePerEndpointTest._
import com.twitter.inject.server.{PortUtils, Ports}
import com.twitter.io.Buf
import com.twitter.scrooge.{HeaderMap, Request, Response}
import com.twitter.serviceA.thriftscala.ServiceA.Echo
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.serviceB.thriftscala.ServiceB.Ping
import com.twitter.util.{Await, Future}

case class InheritanceService(
  ping: Service[Request[Ping.Args], Response[Ping.SuccessType]],
  echo: Service[Request[Echo.Args], Response[Echo.SuccessType]])
    extends ServiceB.ReqRepServicePerEndpoint {

  override def withPing(
    ping: Service[Request[Ping.Args], Response[Ping.SuccessType]]
  ): ServiceB.ReqRepServicePerEndpoint =
    copy(ping = ping)
  override def withEcho(
    echo: Service[Request[com.twitter.serviceA.thriftscala.ServiceA.Echo.Args], Response[
      com.twitter.serviceA.thriftscala.ServiceA.Echo.SuccessType
    ]]
  ): ServiceB.ReqRepServicePerEndpoint =
    copy(echo = echo)

  override def filtered(filter: Filter.TypeAgnostic): ServiceB.ReqRepServicePerEndpoint =
    InheritanceService(ping = filter.toFilter.andThen(ping), echo = filter.toFilter.andThen(echo))
}

class InheritanceThriftTwitterServer(clientRequestHeaderKey: String)
    extends com.twitter.server.TwitterServer
    with Ports {
  private[this] val thriftPortFlag = flag("thrift.port", ":9999", "External Thrift server port")
  private[this] var thriftServer: ListeningServer = NullServer

  private def logRequestHeaders(
    headerMap: HeaderMap,
    assertClientRequestHeaderKey: Boolean = false
  ): Unit = {
    val headers = filteredHeaders(headerMap)
    if (headers.nonEmpty) {
      logger.info(s"Received request headers: ${printHeaders(headers)}")
      if (assertClientRequestHeaderKey) assert(headerMap.contains(clientRequestHeaderKey))
    }
  }

  private val service = InheritanceService(
    ping = Service.mk[Request[Ping.Args], Response[Ping.SuccessType]] {
      request: Request[Ping.Args] =>
        logRequestHeaders(request.headers, assertClientRequestHeaderKey = true)
        Future
          .value(
            Response(
              Map("com.twitter.serviceb.thriftscala.serviceb.ping" -> Seq(Buf.Utf8("response"))),
              "pong"))
    },
    echo = Service.mk[Request[Echo.Args], Response[Echo.SuccessType]] {
      request: Request[Echo.Args] =>
        logRequestHeaders(request.headers)
        Future
          .value(
            Response(
              Map("com.twitter.servicea.thriftscala.servicea.echo" -> Seq(Buf.Utf8("response"))),
              request.args.msg))
    }
  )

  private val serverSideLoggingFilter = loggingFilter {
    logger.info("SERVER-SIDE FILTER")
  }

  def main(): Unit = {
    thriftServer = ThriftMux.server
      .serveIface(
        thriftPortFlag(),
        service
          .withEcho(echo = service.echo)
          .filtered(serverSideLoggingFilter).toThriftService)

    onExit {
      Await.result(thriftServer.close(), 5.seconds)
    }

    Await.ready(thriftServer)
  }

  override def thriftPort: Option[Int] = Option(thriftServer).map(PortUtils.getPort)

}
