package com.twitter.finatra.server

import com.twitter.app.Flag
import com.twitter.conversions.storage._
import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.builder.ServerConfig.Yes
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.service.NullService
import com.twitter.finagle.http.{Response, Request, Http, RichHttp}
import com.twitter.finatra.conversions.string._
import com.twitter.finatra.twitterserver.GuiceTwitterServer
import com.twitter.finatra.utils.PortUtils
import com.twitter.util.{Await, Future, Time}
import java.net.InetSocketAddress

/**
 * -------------------------------
 * FinatraHttpServer Startup Order
 * -------------------------------
 *
 * GuiceApp Trait (init)
 * - Add module flags
 *
 * Lifecycle Trait
 * - Add Mesos lifecycle routes, eg "/health"
 *
 * Warmup Trait
 * - Disable /health
 *
 * AdminHttpServer Trait (premain)
 * - Create admin http server
 *
 * GuiceApp (main)
 * - Create Guice Injector
 * - Call postStartup methods
 * - Call warmup method
 * - Call postWarmup methods
 *
 * FinatraHttpServer Trait (PostWarmup)
 * - Create external http servers
 *
 * GuiceTwitterServer Trait (main)
 * - Enable /health
 * - Await.ready(adminHttpServer)
 */
trait FinatraHttpServer extends GuiceTwitterServer {

  private val httpPortFlag = flag("http.port", ":8888", "External HTTP server port")
  private val maxRequestSize = flag("maxRequestSize", 5.megabytes, "HTTP(s) Max Request Size")
  private val tracingEnabled = flag("tracingEnabled", true, "Tracing enabled")
  private val httpsPortFlag = flag("https.port", "", "HTTPs Port")
  private val certificatePath = flag("cert.path", "", "path to SSL certificate")
  private val keyPath = flag("key.path", "", "path to SSL key")
  private val shutdownTimeout = flag("shutdown.time", 1.minute, "Maximum amount of time to wait for pending requests to complete on shutdown")

  /* Private Mutable State */
  private var httpServer: Server = _
  private var httpsServer: Server = _

  /* Protected */

  protected def httpService: Service[Request, Response] = {
    NullService
  }

  protected def httpCodec: Http = {
    Http()
      .enableTracing(enable = true)
      .maxRequestSize(maxRequestSize())
      .enableTracing(tracingEnabled())
  }

  type FinagleServerBuilder = ServerBuilder[Request, Response, Yes, Yes, Yes]

  protected def configureHttpServer(serverBuilder: FinagleServerBuilder) = {}

  protected def configureHttpsServer(serverBuilder: ServerBuilder[Request, Response, Yes, Yes, Yes]) = {}

  /* Lifecycle */

  override def postWarmup() {
    super.postWarmup()

    startHttpServer()
    startHttpsServer()
  }

  onExit {
    Await.result(
      close(httpServer, shutdownTimeout().fromNow))

    Await.result(
      close(httpsServer, shutdownTimeout().fromNow))
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
        .codec(new RichHttp[Request](httpCodec))
        .bindTo(port)
        .name("http")

      configureHttpServer(serverBuilder)

      httpServer = serverBuilder.build(httpService)
      info("http server started on port: " + httpExternalPort.get)
    }
  }

  private def startHttpsServer() {
    for {
      port <- parsePort(httpsPortFlag)
      certs <- certificatePath.get
      keys <- keyPath.get
    } {
      val serverBuilder = ServerBuilder()
        .codec(new RichHttp[Request](httpCodec))
        .bindTo(port)
        .name("https")
        .tls(
          certs,
          keys)

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
