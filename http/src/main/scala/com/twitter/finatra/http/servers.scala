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

  /**
   * Default external HTTP port used as the [[Flag]] default value for [[httpPortFlag]]. This can be
   * overridden to provide a different default programmatically when a flag value cannot be passed.
   * The format of this value is expected to be a String in the form of ":port".
   *
   * In general, users should prefer setting the [[httpPortFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finatra.http.HttpServerTrait.httpPortFlag]]
   */
  protected def defaultHttpPort: String = ":8888"

  /**
   * External HTTP port [[Flag]]. The default value is specified by [[defaultHttpPort]] which
   * can be overridden to provide a different default.
   *
   * @note the default value is ":8888" as defined by [[defaultHttpPort]].
   * @note the format of this flag is expected to be a String in the form of ":port".
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultHttpPort]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val httpPortFlag =
    flag("http.port", defaultHttpPort, "External HTTP server port")

  /**
   * Default external HTTPS port used as the [[Flag]] default value for [[httpsPortFlag]]. This can
   * be overridden to provide a different default programmatically when a flag value cannot be
   * passed. The format of this value is expected to be a String in the form of ":port".
   *
   * In general, users should prefer setting the [[httpsPortFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finatra.http.HttpServerTrait.httpsPortFlag]]
   */
  protected def defaultHttpsPort: String = ""

  /**
   * External HTTPS port [[Flag]]. The default value is specified by [[defaultHttpsPort]] which
   * can be overridden to provide a different default.
   *
   * @note the default value is "" as defined by [[defaultHttpsPort]].
   * @note the format of this flag is expected to be a String in the form of ":port".
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultHttpsPort]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val httpsPortFlag =
    flag("https.port", defaultHttpsPort, "External HTTPS server port")

  /**
   * Default maximum request message size this server can receive used as the [[Flag]] default value
   * for [[maxRequestSizeFlag]]. This can be overridden to provide a different default
   * programmatically when a flag value cannot be passed. The format of this flag is expected to be
   * a String which is parsable into a [[com.twitter.util.StorageUnit]].
   *
   * In general, users should prefer setting the [[maxRequestSizeFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.util.StorageUnit]]
   * @see [[com.twitter.finagle.Http.Server.withMaxRequestSize]]
   * @see [[com.twitter.finatra.http.HttpServerTrait.maxRequestSizeFlag]]
   */
  protected def defaultMaxRequestSize: StorageUnit = 5.megabytes

  /**
   * Maximum request message [[Flag]]. The default value is specified by [[defaultMaxRequestSize]]
   * which can be overridden to provide a different default.
   *
   * @note the default value is "5.megabytes" as defined by [[defaultMaxRequestSize]].
   * @note the format of this flag is expected to be a String which is parsable into a [[com.twitter.util.StorageUnit]].
   * @see [[com.twitter.util.StorageUnit]]
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultMaxRequestSize]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val maxRequestSizeFlag =
    flag("maxRequestSize", defaultMaxRequestSize, "HTTP(s) Max Request Size")

  /**
   * Default shutdown timeout used as the [[Flag]] default value for [[shutdownTimeoutFlag]]. This
   * represents the deadline for the closing of this server which can be overridden to provide a
   * different default programmatically when a flag value cannot be passed.
   *
   * In general, users should prefer setting the [[shutdownTimeoutFlag]] [[Flag]] value.
   *
   * @note the value is used to denote a delta "from now", that is this value is applied as:
   *       `server.close(shutdownTimeoutDuration.fromNow())`
   * @see [[com.twitter.util.Closable.close(deadline: Time)]]
   * @see [[https://github.com/twitter/util/blob/b0a5d06269b9526b4408239ce1441b2a213dd0df/util-core/src/main/scala/com/twitter/util/Duration.scala#L436]]
   * @see [[com.twitter.finatra.http.HttpServerTrait.shutdownTimeoutFlag]]
   */
  protected def defaultShutdownTimeout: Duration = 1.minute

  /**
   * Shutdown timeout [[Flag]]. The default value is specified by [[defaultShutdownTimeout]] which
   * can be overridden to provide a different default.
   *
   * @note the default value is "1.minute" as defined by [[defaultShutdownTimeout]].
   * @note the format of this flag is expected to be a String which is parsable into a [[com.twitter.util.Duration]].
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultShutdownTimeout]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val shutdownTimeoutFlag = flag(
    "http.shutdown.time",
    defaultShutdownTimeout,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  /**
   * Default server name for the external HTTP interface used as the [[Flag]] default value for
   * [[httpServerNameFlag]]. This can be overridden to provide a different default programmatically
   * when a flag value cannot be passed.
   *
   * In general, users should prefer setting the [[httpServerNameFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finatra.http.HttpServerTrait.httpServerNameFlag]]
   */
  protected def defaultHttpServerName: String = "http"

  /**
   * Server name for the external HTTP interface [[Flag]]. The default value is specified by [[defaultHttpServerName]]
   * which can be overridden to provide a different default.
   *
   * @note the default value is "http" as defined by [[defaultHttpServerName]].
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultHttpServerName]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val httpServerNameFlag =
    flag("http.name", defaultHttpServerName, "Http server name")

  /**
   * Default server name for serving the external HTTPS interface used as the [[Flag]] default value
   * for [[httpsServerNameFlag]]. This can be overridden to provide a different default
   * programmatically when a flag value cannot be passed.
   *
   * In general, users should prefer setting the [[httpsServerNameFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finatra.http.HttpServerTrait.httpsServerNameFlag]]
   */
  protected def defaultHttpsServerName: String = "https"

  /**
   * Server name for the external HTTPS interface [[Flag]]. The default value is specified by [[defaultHttpsServerName]]
   * which can be overridden to provide a different default.
   *
   * @note the default value is "https" as defined by [[defaultHttpsServerName]].
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultHttpsServerName]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val httpsServerNameFlag =
    flag("https.name", defaultHttpsServerName, "Https server name")

  /**
   * Default server announcement String for the HTTP server used as the [[Flag]] default value for
   * [[httpAnnounceFlag]]. This can be overridden to provide a different default programmatically
   * when a flag value cannot be passed. An empty String value is an indication to not perform any
   * announcement of the server.
   *
   * In general, users should prefer setting the [[httpAnnounceFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finagle.ListeningServer.announce(addr: String)]]
   */
  protected def defaultHttpAnnouncement: String = HttpServerTrait.NoHttpAnnouncement

  /**
   * HTTP server announcement String [[Flag]]. The default value is specified by [[defaultHttpAnnouncement]]
   * which can be overridden to provide a different default. Setting an empty String is an indication
   * to not perform any announcement of the server.
   *
   * @note the default value is "No Announcement" (empty String) as defined by [[defaultHttpAnnouncement]].
   * @see [[com.twitter.finagle.ListeningServer.announce(addr: String)]]
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultHttpAnnouncement]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val httpAnnounceFlag = flag[String]("http.announce", defaultHttpAnnouncement,
    "Address for announcing HTTP server. Empty string indicates no announcement.")

  /**
   * Default server announcement String for the HTTPS server used as the [[Flag]] default value for
   * [[httpsAnnounceFlag]]. This can be overridden to provide a different default programmatically
   * when a flag value cannot be passed. An empty String value is an indication to not perform any
   * announcement of the server.
   *
   * In general, users should prefer setting the [[httpsAnnounceFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finagle.ListeningServer.announce(addr: String)]]
   */
  protected def defaultHttpsAnnouncement: String = HttpServerTrait.NoHttpAnnouncement

  /**
   * HTTPS server announcement String [[Flag]]. The default value is specified by [[defaultHttpsAnnouncement]]
   * which can be overridden to provide a different default. Setting an empty String is an indication
   * to not perform any announcement of the server.
   *
   * @note the default value is "No Announcement" (empty String) as defined by [[defaultHttpsAnnouncement]].
   * @see [[com.twitter.finagle.ListeningServer.announce(addr: String)]]
   * @see [[com.twitter.finatra.http.HttpServerTrait.defaultHttpsAnnouncement]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val httpsAnnounceFlag = flag[String]("https.announce", defaultHttpsAnnouncement,
    "Address for announcing HTTPS server. Empty string indicates no announcement.")

  /* Private Mutable State */

  private var httpServer: ListeningServer = NullServer
  private var httpsServer: ListeningServer = NullServer

  /* Abstract */

  /**
   * The Finagle `Service[Request, Response]` to serve on either configured [[ListeningServer]].
   *
   * Users must override with an implementation to serve a `Service[Request, Response]` */
  protected def httpService: Service[Request, Response]

  /* Lifecycle */

  private[this] def defaultHttpServer(name: String): Http.Server = {
    Http.server
      .withMaxRequestSize(maxRequestSizeFlag())
      .withStreaming(streamRequest)
      .withLabel(name)
      .withStatsReceiver(injector.instance[StatsReceiver])
      .withResponseClassifier(injector.instance[HttpResponseClassifier])
  }

  @Lifecycle
  override protected def postWarmup(): Unit = {
    super.postWarmup()

    // START HTTP
    for (address <- parsePort(httpPortFlag)) {
      httpServer =
        build(
          address,
          configureHttpServer(defaultHttpServer(httpServerNameFlag()))
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
      httpsServer =
        build(
          address,
          configureHttpsServer(defaultHttpServer(httpsServerNameFlag())
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
    case _ => Some(PortUtils.getPort(httpServer))
  }

  override def httpsExternalPort: Option[Int] = httpsServer match {
    case NullServer => None
    case _ => Some(PortUtils.getPort(httpsServer))
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

/**
 * A Finagle server which exposes external HTTP or HTTPS interfaces implemented by a
 * `Service[Request, Response]` configured via an [[HttpRouter]]. This trait is
 * intended for use from Scala or with generated Scala code.
 *
 * @note Java users are encouraged to use [[AbstractHttpServer]] instead.
 */
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

  /**
   * Configuration of the `Service[Request, Response]` to serve on the [[ListeningServer]]
   * is defined by configuring the [[HttpRouter]] and not by implementation of this method,
   * thus this method overridden to be final and set to a `NilService`.
   */
  protected final def httpService: Service[Request, Response] = NilService

  /** Serve the `Service[Request, Response]` from the configured [[HttpRouter]]. */
  override protected[http] final def build(addr: InetSocketAddress, server: Http.Server): ListeningServer = {
    val router = injector.instance[HttpRouter]
    AdminHttpRouter.addAdminRoutes(this, router, this.routes)
    server.serve(addr, router.services.externalService)
  }

  /* Abstract */

  /**
   * Users MUST provide an implementation to configure the provided [[HttpRouter]]. The [[HttpRouter]]
   * exposes a DSL which results in a configured Finagle `Service[-Request, +Response]` to serve on
   * the [[ListeningServer]].
   *
   * @param router the [[HttpRouter]] to configure.
   */
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

/**
 * A Finagle server which exposes an external HTTP or HTTPS interfaces implemented by a
 * `Service[Request, Response]` configured via an [[HttpRouter]]. This abstract class is
 * intended for use from Java or with generated Java code.
 *
 * @note Scala users are encouraged to use [[HttpServer]] instead.
 */
abstract class AbstractHttpServer extends HttpServer
