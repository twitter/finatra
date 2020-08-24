package com.twitter.finatra.http.benchmark

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.http.{HttpMuxer, Request, Response}
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter}
import com.twitter.inject.server.Ports
import com.twitter.io.Buf
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future, TwitterDateFormat}
import java.net.InetSocketAddress
import java.text.DateFormat
import java.util.{Date, Locale}

object FinagleBenchmarkServerMain extends FinagleBenchmarkServer

class FinagleBenchmarkServer extends TwitterServer with Ports {

  private[this] val httpPortFlag = flag("http.port", ":0", "External HTTP server port")
  private[this] val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private[this] val dateFormat: ThreadLocal[DateFormat] = new ThreadLocal[DateFormat] {
    override def initialValue: DateFormat =
      TwitterDateFormat("E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
  }
  private[this] val helloWorld: Buf = Buf.Utf8("Hello, World!")

  private[this] val muxer: HttpMuxer = new HttpMuxer()
    .withHandler(
      "/",
      Service.mk { _: Request =>
        val rep = Response()
        rep.content =
          Buf.ByteArray.Owned(mapper.writeValueAsBytes(Map("message" -> "Hello, World!")))
        rep.contentType = "application/json"

        Future.value(rep)
      }
    )
    .withHandler(
      "/plaintext",
      Service.mk { req: Request =>
        val rep = Response()
        rep.content = helloWorld
        rep.contentType = "text/plain"

        Future.value(rep)
      })

  private[this] val serverAndDate: SimpleFilter[Request, Response] =
    new SimpleFilter[Request, Response] {
      private[this] val addServerAndDate: Response => Response = { rep =>
        rep.headerMap.set("Server", "Finagle")
        rep.headerMap.set("Date", dateFormat.get.format(new Date()))
        rep
      }

      def apply(req: Request, s: Service[Request, Response]): Future[Response] =
        s(req).map(addServerAndDate)
    }

  lazy val server: ListeningServer = Http.server
    .withCompressionLevel(0)
    .withStatsReceiver(NullStatsReceiver)
    .withTracer(NullTracer)
    .serve(httpPortFlag(), serverAndDate.andThen(muxer))
  closeOnExit(server)

  override def httpExternalPort: Option[Int] = {
    Some(server.boundAddress.asInstanceOf[InetSocketAddress].getPort)
  }

  final def main(): Unit = {
    Await.ready(server)
  }
}
