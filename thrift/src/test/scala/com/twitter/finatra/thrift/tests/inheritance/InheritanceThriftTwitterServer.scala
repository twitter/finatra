package com.twitter.finatra.thrift.tests.inheritance

import com.twitter.conversions.time._
import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.{Filter, ListeningServer, NullServer, Service, ThriftMux}
import com.twitter.inject.server.{PortUtils, Ports}
import com.twitter.io.Buf
import com.twitter.scrooge.{HeaderMap, Request, Response}
import com.twitter.serviceA.thriftscala.ServiceA.Echo
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.serviceB.thriftscala.ServiceB.Ping
import com.twitter.util.logging.Logger
import com.twitter.util.{Await, Future}

case class InheritanceService(
  ping: Service[Request[Ping.Args], Response[Ping.SuccessType]],
  echo: Service[Request[Echo.Args], Response[Echo.SuccessType]]
) extends ServiceB.ReqRepServicePerEndpoint {

  override def withPing(
    ping : Service[Request[Ping.Args], Response[Ping.SuccessType]]
  ): ServiceB.ReqRepServicePerEndpoint =
    copy(ping = ping)
  override def withEcho(
    echo : Service[Request[com.twitter.serviceA.thriftscala.ServiceA.Echo.Args], Response[com.twitter.serviceA.thriftscala.ServiceA.Echo.SuccessType]]
  ): ServiceB.ReqRepServicePerEndpoint =
    copy(echo = echo)

  override def filtered(filter: Filter.TypeAgnostic): ServiceB.ReqRepServicePerEndpoint =
    InheritanceService(
      ping = filter.toFilter.andThen(ping),
      echo = filter.toFilter.andThen(echo))
}

class InheritanceThriftTwitterServer extends com.twitter.server.TwitterServer with Ports {
  override val name = "inheritance-thrift-twitterserver"

  private[this] val thriftPortFlag = flag("thrift.port", ":9999", "External Thrift server port")
  private[this] var thriftServer: ListeningServer = NullServer

  private[this] val logger = Logger(getClass.getName)

  private def printHeaders(headers: HeaderMap, filter: Boolean = true): String = {
    val filtered = if (filter) {
      headers.toMap.filterKeys(key => !key.startsWith("com.twitter.finagle"))
    } else {
      headers.toMap.filterKeys(_ => true)
    }
    filtered.mapValues(values => values.map(Buf.Utf8.unapply(_).get)).toString()
  }

  private[this] val service = InheritanceService(
    ping = Service.mk[Request[Ping.Args], Response[Ping.SuccessType]] { request: Request[Ping.Args] =>
      logger.info(s"Received request headers: ${ printHeaders(request.headers) }")
      Future
        .value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.ping" -> Seq(Buf.Utf8("response"))), "pong"))
    },
    echo = Service.mk[Request[Echo.Args], Response[Echo.SuccessType]] { request: Request[Echo.Args] =>
      logger.info(s"Received request headers: ${printHeaders(request.headers)}")
      assert(request.headers.contains("com.twitter.client123.foo"))
      Future
        .value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.echo" -> Seq(Buf.Utf8("response"))), request.args.msg))
    }
  )

  private val serverSideLoggingFilter = new TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        def apply(
          request: Req,
          service: Service[Req, Rep]
        ): Future[Rep] = {
          logger.info("SERVER-SIDE FILTER")
          service(request)
        }
      }
    }
  }

  def main(): Unit = {
    thriftServer =
      ThriftMux.server
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
