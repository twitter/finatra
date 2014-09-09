package com.twitter.finatra

import com.twitter.conversions.storage._
import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.service.NullService
import com.twitter.finagle.http.{Http, Response, RichHttp, Request => FinatraRequest}
import com.twitter.finatra.twitterserver.GuiceTwitterServer
import com.twitter.finatra.utils.PortUtils
import com.twitter.finatra.utils.PortUtils._
import com.twitter.util.{Await, Future, Time}
import java.net.InetSocketAddress

/**
 * -------------------------
 * Startup Order
 * -------------------------
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
 * GuiceApp (premain)
 * - Create Guice Injector
 *
 * GuiceApp Trait (main)
 * - Call postStartup methods
 * - Call warmup method
 * - Call postWarmup methods
 *
 * FinatraRawServer Trait (PostWarmup)
 * - Create external http servers
 *
 * GuiceTwitterServer Trait (main)
 * - Enable /health
 * - Await.ready(adminHttpServer)
 */
trait FinatraRawServer extends GuiceTwitterServer {

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

  protected def httpService: Service[FinatraRequest, Response] = {
    NullService
  }

  protected def httpCodec: Http = {
    Http()
      .enableTracing(enable = true)
      .maxRequestSize(maxRequestSize())
      .enableTracing(tracingEnabled())
  }

  /* Lifecycle */

  override def postWarmup() {
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

  override def httpExternalPort = Option(httpServer) map getPort

  override def httpExternalSocketAddress = Option(httpServer) map getSocketAddress

  override def httpsExternalPort = Option(httpsServer) map getPort

  /* Private */

  /* We parse the port as a string, so that clients can
     set the port to "" to prevent a http server from being started */
  private def httpPort: Option[InetSocketAddress] = {
    val port = httpPortFlag()
    if (port.isEmpty)
      None
    else
      Some(PortUtils.parseAddr(port))
  }

  private def startHttpServer() {
    for (port <- httpPort) {
      httpServer = ServerBuilder()
        .codec(new RichHttp[FinatraRequest](httpCodec))
        .bindTo(port)
        .name("http")
        .build(httpService)

      info("http server started on port: " + httpExternalPort.get)
    }
  }

  private def startHttpsServer() {
    for {
      port <- httpsPortFlag.get
      certs <- certificatePath.get
      keys <- keyPath.get
    } {
      httpsServer = ServerBuilder()
        .codec(new RichHttp[FinatraRequest](httpCodec))
        .bindTo(PortUtils.parseAddr(httpsPortFlag()))
        .name("https")
        .tls(
          certs,
          keys)
        .build(httpService)

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
