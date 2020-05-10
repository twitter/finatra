package com.twitter.inject.server.tests

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Logging
import com.twitter.util.{Await, Duration, Future}

class NonInjectionTestServer(
  statsReceiver: Option[StatsReceiver])
    extends com.twitter.server.TwitterServer
    with Logging {

  private[this] val portFlag = flag("http.port", ":8888", "HTTP port")

  override val defaultCloseGracePeriod: Duration = 2.seconds

  def this() = this(None)

  private[this] val service = new Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response(request.version, Status.Ok)
      response.contentString = "hello"
      Future.value(response)
    }
  }

  def main(): Unit = {
    val builder: Http.Server = statsReceiver match {
      case Some(sr) => Http.server.withStatsReceiver(sr)
      case _ => Http.server
    }

    val server: ListeningServer = builder.serve(portFlag(), service)
    onExit {
      server.close()
    }
    Await.all(adminHttpServer, server)
  }
}
