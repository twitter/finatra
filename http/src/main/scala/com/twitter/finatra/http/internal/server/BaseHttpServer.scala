package com.twitter.finatra.http.internal.server

import com.twitter.app.Flag
import com.twitter.conversions.storage._
import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.builder.ServerConfig.Yes
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.service.NullService
import com.twitter.finagle.http.{Http, Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.conversions.string._
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util._
import java.net.InetSocketAddress

trait BaseHttpServer extends TwitterServer {

  protected def defaultFinatraHttpPort: String = ":8888"
  private val httpPortFlag = flag("http.port", defaultFinatraHttpPort, "External HTTP server port")

  protected def defaultMaxRequestSize: StorageUnit = 5.megabytes
  private val maxRequestSizeFlag = flag("maxRequestSize", defaultMaxRequestSize, "HTTP(s) Max Request Size")

  protected def defaultTracingEnabled: Boolean = true
  private val tracingEnabledFlag = flag("tracingEnabled", defaultTracingEnabled, "Tracing enabled")

  protected def defaultHttpsPort: String = ""
  private val httpsPortFlag = flag("https.port", defaultHttpsPort, "HTTPs Port")

  protected def defaultCertificatePath: String = ""
  private val certificatePathFlag = flag("cert.path", defaultCertificatePath, "path to SSL certificate")

  protected def defaultKeyPath: String = ""
  private val keyPathFlag = flag("key.path", defaultKeyPath, "path to SSL key")

  protected def defaultShutdownTimeout: Duration = 1.minute
  private val shutdownTimeoutFlag = flag("shutdown.time", defaultShutdownTimeout, "Maximum amount of time to wait for pending requests to complete on shutdown")

  private val httpAnnounceFlag = flag[String]("http.announce", "Address for announcing HTTP server")

  private val httpsAnnounceFlag = flag[String]("https.announce", "Address for announcing HTTPS server")

  protected def defaultHttpServerName: String = "http"
  private val httpServerNameFlag = flag("http.name", defaultHttpServerName, "Http server name")

  protected def defaultHttpsServerName: String = "https"
  private val httpsServerNameFlag = flag("https.name", defaultHttpsServerName, "Https server name")

  /* Private Mutable State */

  private var httpServer: Server = _
  private var httpsServer: Server = _

  /* Protected */


  protected def httpService: Service[Request, Response] = {
    NullService
  }

  protected def httpCodec: Http = {
    Http()
      .maxRequestSize(maxRequestSizeFlag())
      .enableTracing(tracingEnabledFlag())
      .streaming(streamRequest)
  }

  /**
   * If false, the underlying Netty pipeline collects HttpChunks into the body of each Request
   * Set to true if you wish to stream parse requests using request.reader.read
   */
  protected def streamRequest: Boolean = false

  type FinagleServerBuilder = ServerBuilder[Request, Response, Yes, Yes, Yes]

  protected def configureHttpServer(serverBuilder: FinagleServerBuilder) = {}

  protected def configureHttpsServer(serverBuilder: FinagleServerBuilder) = {}

  /* Lifecycle */

  override def postWarmup() {
    super.postWarmup()

    startHttpServer()
    startHttpsServer()
  }

  override def waitForServer() {
    if (httpServer != null) {
      Await.ready(httpServer)
    } else {
      Await.ready(httpsServer)
    }
  }

  /* Overrides */

  override def httpExternalPort = Option(httpServer) map PortUtils.getPort

  override def httpsExternalPort = Option(httpsServer) map PortUtils.getPort

  /* Private */

  /* We parse the port as a string, so that clients can
     set the port to "" to prevent a http server from being started */
  private def parsePort(port: Flag[String]): Option[InetSocketAddress] = {
    port().toOption map PortUtils.parseAddr
  }

  private def startHttpServer() {
    for (port <- parsePort(httpPortFlag)) {
      val serverBuilder = ServerBuilder()
        .codec(httpCodec)
        .bindTo(port)
        .reportTo(injector.instance[StatsReceiver])
        .name(httpServerNameFlag())

      configureHttpServer(serverBuilder)

      httpServer = serverBuilder.build(httpService)
      onExit {
        Await.result(
          close(httpServer, shutdownTimeoutFlag().fromNow))
      }
      for (addr <- httpAnnounceFlag.get) httpServer.announce(addr)
      info("http server started on port: " + httpExternalPort.get)
    }
  }

  private def startHttpsServer() {
    for (port <- parsePort(httpsPortFlag)) {
      val serverBuilder = ServerBuilder()
        .codec(httpCodec)
        .bindTo(port)
        .reportTo(injector.instance[StatsReceiver])
        .name(httpsServerNameFlag())
        .tls(
          certificatePathFlag(),
          keyPathFlag())

      configureHttpsServer(serverBuilder)

      httpsServer = serverBuilder.build(httpService)
      onExit {
        Await.result(
          close(httpsServer, shutdownTimeoutFlag().fromNow))
      }
      for (addr <- httpsAnnounceFlag.get) httpsServer.announce(addr)
      info("https server started on port: " + httpsExternalPort)
    }
  }

  private def close(server: Server, deadline: Time) = {
    if (server != null)
      server.close(deadline)
    else
      Future.Unit
  }
}
