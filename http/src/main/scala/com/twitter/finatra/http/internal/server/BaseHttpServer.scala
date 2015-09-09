package com.twitter.finatra.http.internal.server

import com.twitter.app.Flag
import com.twitter.conversions.storage._
import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.builder.ServerConfig.Yes
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.httpx.service.NullService
import com.twitter.finagle.httpx.{Http, Request, Response}
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

  onExit {
    Await.result(
      close(httpServer, shutdownTimeoutFlag().fromNow))

    Await.result(
      close(httpsServer, shutdownTimeoutFlag().fromNow))
  }

  /* Overrides */

  override def httpExternalPort = Option(httpServer) map PortUtils.getPort

  override def httpExternalSocketAddress = Option(httpServer) map PortUtils.getSocketAddress

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
        .name("http")

      configureHttpServer(serverBuilder)

      httpServer = serverBuilder.build(httpService)
      info("http server started on port: " + httpExternalPort.get)
    }
  }

  private def startHttpsServer() {
    for (port <- parsePort(httpsPortFlag)) {
      val serverBuilder = ServerBuilder()
        .codec(httpCodec)
        .bindTo(port)
        .reportTo(injector.instance[StatsReceiver])
        .name("https")
        .tls(
          certificatePathFlag(),
          keyPathFlag())

      configureHttpsServer(serverBuilder)

      httpsServer = serverBuilder.build(httpService)
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
