package com.twitter.finatra.http.internal.server

import com.google.inject.Module
import com.twitter.app.Flag
import com.twitter.conversions.storage._
import com.twitter.conversions.time._
import com.twitter.finagle.http.service.NullService
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Http, ListeningServer, NullServer, Service}
import com.twitter.finatra.http.modules.HttpResponseClassifierModule
import com.twitter.finatra.http.response.HttpResponseClassifier
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.conversions.string._
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util._
import java.net.InetSocketAddress

private object BaseHttpServer {
  /**
   * Sentinel used to indicate no http/https announcement.
   */
  val NoHttpAnnouncement: String = ""
}

private[http] trait BaseHttpServer extends TwitterServer {

  /** Add Framework Modules */
  addFrameworkModule(httpResponseClassifierModule)

  protected def defaultHttpPort: String = ":8888"
  private val httpPortFlag = flag("http.port", defaultHttpPort, "External HTTP server port")

  protected def defaultMaxRequestSize: StorageUnit = 5.megabytes
  private val maxRequestSizeFlag =
    flag("maxRequestSize", defaultMaxRequestSize, "HTTP(s) Max Request Size")

  protected def defaultHttpsPort: String = ""
  private val httpsPortFlag = flag("https.port", defaultHttpsPort, "External HTTPS server port")

  protected def defaultShutdownTimeout: Duration = 1.minute
  private val shutdownTimeoutFlag = flag(
    "shutdown.time",
    defaultShutdownTimeout,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  protected def defaultHttpAnnouncement: String = BaseHttpServer.NoHttpAnnouncement
  private val httpAnnounceFlag = flag[String]("http.announce", defaultHttpAnnouncement,
    "Address for announcing HTTP server. Empty string indicates no announcement.")

  protected def defaultHttpsAnnouncement: String = BaseHttpServer.NoHttpAnnouncement
  private val httpsAnnounceFlag = flag[String]("https.announce", defaultHttpsAnnouncement,
    "Address for announcing HTTPS server. Empty string indicates no announcement.")

  protected def defaultHttpServerName: String = "http"
  private val httpServerNameFlag = flag("http.name", defaultHttpServerName, "Http server name")

  protected def defaultHttpsServerName: String = "https"
  private val httpsServerNameFlag = flag("https.name", defaultHttpsServerName, "Https server name")

  /* Private Mutable State */

  private var httpServer: ListeningServer = NullServer
  private var httpsServer: ListeningServer = NullServer

  private lazy val baseHttpServer: Http.Server = {
    Http.server
      .withMaxRequestSize(maxRequestSizeFlag())
      .withStreaming(streamRequest)
  }

  /* Protected */

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing an [[HttpResponseClassifier]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides an [[HttpResponseClassifier]] implementation.
   */
  protected def httpResponseClassifierModule: Module = HttpResponseClassifierModule

  protected def httpService: Service[Request, Response] = {
    NullService
  }

  /**
   * If false, the underlying Netty pipeline collects HttpChunks into the body of each Request
   * Set to true if you wish to stream parse requests using request.reader.read
   */
  protected def streamRequest: Boolean = false

  /**
   * This method allows for further configuration of the http server for parameters not exposed by
   * this trait or for overriding defaults provided herein, e.g.,
   *
   * override def configureHttpServer(server: Http.Server): Http.Server = {
   *  server
   *    .withMaxInitialLineSize(2048)
   * }
   *
   * @param server - the [[com.twitter.finagle.Http.Server]] to configure.
   *
   * @return a configured Http.Server.
   */
  protected def configureHttpServer(server: Http.Server): Http.Server = {
    server
  }

  /**
   * This method allows for further configuration of the https server for parameters not exposed by
   * this trait or for overriding defaults provided herein, e.g.,
   *
   * override def configureHttpsServer(server: Http.Server): Http.Server = {
   *  server
   *    .withMaxInitialLineSize(2048)
   *    .withTransport.tls(....)
   * }
   *
   * @param server - the [[com.twitter.finagle.Http.Server]] to configure.
   *
   * @return a configured Http.Server.
   */
  protected def configureHttpsServer(server: Http.Server): Http.Server = {
    server
  }

  /* Lifecycle */

  @Lifecycle
  override protected def postWarmup(): Unit = {
    super.postWarmup()

    startHttpServer()
    startHttpsServer()
  }

  /* Overrides */

  override def httpExternalPort: Option[Int] = httpServer match {
    case NullServer => None
    case _ => Option(httpServer).map(PortUtils.getPort)
  }

  override def httpsExternalPort: Option[Int] = httpsServer match {
    case NullServer => None
    case _ => Option(httpsServer).map(PortUtils.getPort)
  }

  /* Private */

  /* We parse the port as a string, so that clients can
     set the port to "" to prevent a http server from being started */
  private def parsePort(port: Flag[String]): Option[InetSocketAddress] = {
    port().toOption.map(PortUtils.parseAddr)
  }

  private def startHttpServer(): Unit = {
    for (port <- parsePort(httpPortFlag)) {
      val serverBuilder =
        configureHttpServer(
          baseHttpServer
            .withLabel(httpServerNameFlag())
            .withStatsReceiver(injector.instance[StatsReceiver])
            .withResponseClassifier(injector.instance[HttpResponseClassifier])
        )

      httpServer = serverBuilder.serve(port, httpService)
      onExit {
        Await.result(httpServer.close(shutdownTimeoutFlag().fromNow))
      }
      await(httpServer)
      httpAnnounceFlag() match {
        case BaseHttpServer.NoHttpAnnouncement => // no-op
        case addr =>
          info(s"http server announced to $addr")
          httpServer.announce(addr)
      }
      info(s"http server started on port: $port")
    }
  }

  private def startHttpsServer(): Unit = {
    for (port <- parsePort(httpsPortFlag)) {
      val serverBuilder =
        configureHttpsServer(
          baseHttpServer
            .withLabel(httpsServerNameFlag())
            .withStatsReceiver(injector.instance[StatsReceiver])
            .withResponseClassifier(injector.instance[HttpResponseClassifier])
        )

      httpsServer = serverBuilder.serve(port, httpService)
      onExit {
        Await.result(httpsServer.close(shutdownTimeoutFlag().fromNow))
      }
      await(httpsServer)
      httpsAnnounceFlag() match {
        case BaseHttpServer.NoHttpAnnouncement => // no-op
        case addr =>
          info(s"https server announced to $addr")
          httpsServer.announce(addr)
      }
      info(s"https server started on port: $port")
    }
  }
}
