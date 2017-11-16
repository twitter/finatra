package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.conversions.time._
import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.doeverything.thriftscala.DoEverything.{Ask, Echo, Echo2, MagicNum, MoreThanTwentyTwoArgs, Uppercase}
import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.{Filter, ListeningServer, NullServer, Service, ThriftMux}
import com.twitter.inject.server.{PortUtils, Ports}
import com.twitter.io.Buf
import com.twitter.scrooge.{HeaderMap, Request, Response}
import com.twitter.util.{Await, Future}
import com.twitter.util.logging.Logger

class DoEverythingThriftTwitterServer extends com.twitter.server.TwitterServer with Ports {
  override val name = "doeverything-thrift-twitterserver"

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

  private[this] val service = DoEverything.ReqRepServicePerEndpoint(
    uppercase = Service.mk[Request[Uppercase.Args], Response[Uppercase.SuccessType]] { request: Request[Uppercase.Args] =>
      logger.info(s"Received request headers: ${printHeaders(request.headers)}")
      assert(request.headers.contains("com.twitter.client123.foo"))
      Future.value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.uppercase" -> Seq(Buf.Utf8("response"))), request.args.msg.asInstanceOf[String].toUpperCase))
    },
    echo = Service.mk[Request[Echo.Args], Response[Echo.SuccessType]] { request: Request[Echo.Args] =>
      logger.info(s"Received request headers: ${printHeaders(request.headers)}")
      assert(request.headers.contains("com.twitter.client123.foo"))
      Future.value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.echo" -> Seq(Buf.Utf8("response"))), request.args.msg))
    },
    echo2 = Service.mk[Request[Echo2.Args], Response[Echo2.SuccessType]] { request: Request[Echo2.Args] =>
      Future.value(Response(request.args.msg))
    },
    magicNum = Service.mk[Request[MagicNum.Args], Response[MagicNum.SuccessType]] { request: Request[MagicNum.Args] =>
      Future.value(Response("42"))
    },
    moreThanTwentyTwoArgs = Service.mk[Request[MoreThanTwentyTwoArgs.Args], Response[MoreThanTwentyTwoArgs.SuccessType]]{ request: Request[MoreThanTwentyTwoArgs.Args] =>
      Future.value(Response(""))
    },
    ask = Service.mk[Request[Ask.Args], Response[Ask.SuccessType]]{ request: Request[Ask.Args] =>
      Future.value(Response(com.twitter.doeverything.thriftscala.Answer("42")))
    }
  )

  private val serverSideLoggingFilter = new TypeAgnostic {
    override def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        override def apply(
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
          service.filtered(serverSideLoggingFilter).toThriftService)

    onExit {
      Await.result(thriftServer.close(), 5.seconds)
    }

    Await.ready(thriftServer)
  }

  override def thriftPort: Option[Int] = Option(thriftServer).map(PortUtils.getPort)
}
