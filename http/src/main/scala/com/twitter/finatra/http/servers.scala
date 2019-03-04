package com.twitter.finatra.http

import com.google.inject.Module
import com.twitter.app.Flag
import com.twitter.conversions.StorageUnitOps._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.NilService
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.internal.routing.AdminHttpRouter
import com.twitter.finatra.http.modules._
import com.twitter.finatra.http.response.HttpResponseClassifier
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.conversions.string._
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.{Await, Duration, StorageUnit}
import java.net.InetSocketAddress

private object HttpServerTrait {
  /**
   * Sentinel used to indicate no http/https announcement.
   */
  val NoHttpAnnouncement: String = ""
}

/**
 * A basic HttpServer. To implement, override
 * {{{
 *   protected def httpService: Service[Request, Response]
 * }}}
 *
 * with your `Service[Request, Response]` implementation.
 */
trait HttpServerTrait extends TwitterServer {

  /** Add Framework Modules */
  addFrameworkModule(
    httpResponseClassifierModule)

  /** Http Port */
  protected def defaultHttpPort: String = ":8888"
  private val httpPortFlag = flag("http.port", defaultHttpPort, "External HTTP server port")

  /** Https Port */
  protected def defaultHttpsPort: String = ""
  private val httpsPortFlag = flag("https.port", defaultHttpsPort, "External HTTPS server port")

  /** HTTP Max Request Message Size */
  protected def defaultMaxRequestSize: StorageUnit = 5.megabytes
  private val maxRequestSizeFlag =
    flag("maxRequestSize", defaultMaxRequestSize, "HTTP(s) Max Request Size")

  /** Shutdown Timeout */
  protected def defaultShutdownTimeout: Duration = 1.minute
  private val shutdownTimeoutFlag = flag(
    "shutdown.time", // todo: rename to http.shutdown.time
    defaultShutdownTimeout,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  /** HTTP Server Name */
  protected def defaultHttpServerName: String = "http"
  private val httpServerNameFlag = flag("http.name", defaultHttpServerName, "Http server name")

  /** HTTPS Server Name */
  protected def defaultHttpsServerName: String = "https"
  private val httpsServerNameFlag = flag("https.name", defaultHttpsServerName, "Https server name")

  /** HTTP Server Announcement */
  protected def defaultHttpAnnouncement: String = HttpServerTrait.NoHttpAnnouncement
  private val httpAnnounceFlag = flag[String]("http.announce", defaultHttpAnnouncement,
    "Address for announcing HTTP server. Empty string indicates no announcement.")

  /** HTTPS Server Announcement */
  protected def defaultHttpsAnnouncement: String = HttpServerTrait.NoHttpAnnouncement
  private val httpsAnnounceFlag = flag[String]("https.announce", defaultHttpsAnnouncement,
    "Address for announcing HTTPS server. Empty string indicates no announcement.")

  /* Private Mutable State */

  private var httpServer: ListeningServer = NullServer
  private var httpsServer: ListeningServer = NullServer

  /* Abstract */

  /** Override with an implementation to serve a Service[Request, Response] */
  protected def httpService: Service[Request, Response]

  /* Lifecycle */

  @Lifecycle
  override protected def postWarmup(): Unit = {
    super.postWarmup()

    // START HTTP
    for (address <- parsePort(httpPortFlag)) {
      httpServer = build(
        address,
        configureHttpServer(
          Http.server
            .withMaxRequestSize(maxRequestSizeFlag())
            .withStreaming(streamRequest)
            .withLabel(httpServerNameFlag())
            .withStatsReceiver(injector.instance[StatsReceiver])
            .withResponseClassifier(injector.instance[HttpResponseClassifier])
        )
      )

      onExit {
        Await.result(httpServer.close(shutdownTimeoutFlag().fromNow))
      }
      await(httpServer)
      httpAnnounceFlag() match {
        case HttpServerTrait.NoHttpAnnouncement => // no-op
        case addr =>
          info(s"http server announced to $addr")
          httpServer.announce(addr)
      }
      info(s"http server started on port: $address")
    }

    // START HTTPS
    for (address <- parsePort(httpsPortFlag)) {
      httpsServer = build(
        address,
        configureHttpsServer(
          Http.server
            .withMaxRequestSize(maxRequestSizeFlag())
            .withStreaming(streamRequest)
            .withLabel(httpsServerNameFlag())
            .withStatsReceiver(injector.instance[StatsReceiver])
            .withResponseClassifier(injector.instance[HttpResponseClassifier])
        )
      )

      onExit {
        Await.result(httpsServer.close(shutdownTimeoutFlag().fromNow))
      }
      await(httpsServer)
      httpsAnnounceFlag() match {
        case HttpServerTrait.NoHttpAnnouncement => // no-op
        case addr =>
          info(s"https server announced to $addr")
          httpsServer.announce(addr)
      }
      info(s"https server started on port: $address")
    }
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

  /* Protected */

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing an [[HttpResponseClassifier]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides an [[HttpResponseClassifier]] implementation.
   */
  protected def httpResponseClassifierModule: Module = HttpResponseClassifierModule

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

  /**
   * Construct a [[com.twitter.finagle.ListeningServer]] from the given [[InetSocketAddress]] addr
   * and configured [[Http.Server]] stack.
   *
   * @param addr the [[InetSocketAddress]] address to bind the resultant [[ListeningServer]].
   * @param server the configured [[Http.Server]] stack.
   * @return a constructed [[ListeningServer]].
   */
  protected[http] def build(addr: InetSocketAddress, server: Http.Server): ListeningServer = {
    server.serve(addr, this.httpService)
  }

  /* Private */

  // We parse the port as a string, so that clients can set the port to empty string
  // to prevent a http server from being started.
  private def parsePort(port: Flag[String]): Option[InetSocketAddress] = {
    port().toOption.map(PortUtils.parseAddr)
  }
}

/** HttpServer for use from Scala */
trait HttpServer extends HttpServerTrait {

  /** Add Framework Modules */
  addFrameworkModules(
    accessLogModule,
    DocRootModule,
    ExceptionManagerModule,
    jacksonModule,
    messageBodyModule,
    mustacheModule
  )

  /** This Server does not return a `Service[Request, Response]` */
  protected final def httpService: Service[Request, Response] = NilService

  /** Instead the [[HttpRouter]] provides the `Service[Request, Response]` to serve. */
  override protected[http] final def build(addr: InetSocketAddress, server: Http.Server): ListeningServer = {
    val router = injector.instance[HttpRouter]
    AdminHttpRouter.addAdminRoutes(this, router, this.routes)
    server.serve(addr, router.services.externalService)
  }

  /* Abstract */

  protected def configureHttp(router: HttpRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    configureHttp(injector.instance[HttpRouter])
  }

  /* Protected */

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finagle.filter.LogFormatter]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finagle.filter.LogFormatter]] implementation.
   */
  protected def accessLogModule: Module = AccessLogModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.github.mustachejava.MustacheFactory]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.github.mustachejava.MustacheFactory]] implementation.
   */
  protected def mustacheModule: Module = MustacheModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing implementations for a
   * [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] and a
   * [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides implementations for
   *         [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] and
   *         [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
   */
  protected def messageBodyModule: Module = MessageBodyModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finatra.json.FinatraObjectMapper]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finatra.json.FinatraObjectMapper]] implementation.
   */
  protected def jacksonModule: Module = FinatraJacksonModule
}

/** AbstractHttpServer for usage from Java */
abstract class AbstractHttpServer extends HttpServer
