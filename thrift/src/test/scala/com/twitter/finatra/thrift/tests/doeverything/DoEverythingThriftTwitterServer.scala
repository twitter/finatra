package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.conversions.time._
import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.doeverything.thriftscala.DoEverything.{Ask, Echo, Echo2, MagicNum, MoreThanTwentyTwoArgs, Uppercase}
import com.twitter.finagle.{ListeningServer, NullServer, Service, ThriftMux}
import com.twitter.finatra.thrift.tests.ReqRepServicePerEndpointTest._
import com.twitter.inject.server.{PortUtils, Ports}
import com.twitter.io.Buf
import com.twitter.scrooge.{HeaderMap, Request, Response}
import com.twitter.util.logging.Logger
import com.twitter.util.{Await, Future}

class DoEverythingThriftTwitterServer(clientRequestHeaderKey: String) extends com.twitter.server.TwitterServer with Ports {
  private[this] val thriftPortFlag = flag("thrift.port", ":9999", "External Thrift server port")
  private[this] var thriftServer: ListeningServer = NullServer

  private[this] val logger = Logger(getClass.getName)

  private def logRequestHeaders(headerMap: HeaderMap, assertClientRequestHeaderKey: Boolean = false): Unit = {
    val headers = filteredHeaders(headerMap)
    if (headers.nonEmpty) {
      logger.info(s"Received request headers: ${printHeaders(headers)}")
      if (assertClientRequestHeaderKey) assert(headerMap.contains(clientRequestHeaderKey))
    }
  }

  private val service = DoEverything.ReqRepServicePerEndpoint(
    uppercase = Service.mk[Request[Uppercase.Args], Response[Uppercase.SuccessType]] { request: Request[Uppercase.Args] =>
      logRequestHeaders(request.headers, assertClientRequestHeaderKey = true)
      Future.value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.uppercase" -> Seq(Buf.Utf8("response"))), request.args.msg.asInstanceOf[String].toUpperCase))
    },
    echo = Service.mk[Request[Echo.Args], Response[Echo.SuccessType]] { request: Request[Echo.Args] =>
      logRequestHeaders(request.headers, assertClientRequestHeaderKey = true)
      Future.value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.echo" -> Seq(Buf.Utf8("response"))), request.args.msg))
    },
    echo2 = Service.mk[Request[Echo2.Args], Response[Echo2.SuccessType]] { request: Request[Echo2.Args] =>
      logRequestHeaders(request.headers)
      Future.value(Response(request.args.msg))
    },
    magicNum = Service.mk[Request[MagicNum.Args], Response[MagicNum.SuccessType]] { request: Request[MagicNum.Args] =>
      logRequestHeaders(request.headers)
      Future.value(Response(Map("com.twitter.doeverything.thriftscala.doeverything.magicNum" -> Seq(Buf.Utf8("response"))), "42"))
    },
    moreThanTwentyTwoArgs = Service.mk[Request[MoreThanTwentyTwoArgs.Args], Response[MoreThanTwentyTwoArgs.SuccessType]]{ request: Request[MoreThanTwentyTwoArgs.Args] =>
      logRequestHeaders(request.headers)
      Future.value(Response(""))
    },
    ask = Service.mk[Request[Ask.Args], Response[Ask.SuccessType]]{ request: Request[Ask.Args] =>
      logRequestHeaders(request.headers)
      Future.value(Response(com.twitter.doeverything.thriftscala.Answer("42")))
    }
  )

  private val serverSideLoggingFilter = loggingFilter {
    logger.info("SERVER-SIDE FILTER")
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
