package com.twitter.finatra.thrift

import com.google.inject.Module
import com.twitter.app.Flag
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.service.NilService
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{ListeningServer, NullServer, Service, ThriftMux}
import com.twitter.finatra.thrift.modules.{ExceptionManagerModule, ThriftResponseClassifierModule}
import com.twitter.finatra.thrift.response.ThriftResponseClassifier
import com.twitter.finatra.thrift.routing.{JavaThriftRouter, ThriftRouter}
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.internal.LibraryRegistry
import com.twitter.inject.modules.StackTransformerModule
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.{Await, Duration}

private object ThriftServerTrait {

  /**
   * Sentinel used to indicate no announcement.
   */
  val NoThriftAnnouncement: String = ""
}

/**
 * A basic ThriftServer. To implement, override
 * {{{
 *   protected def thriftService: Service[Array[Byte], Array[Byte]]
 * }}}
 *
 * with your `Service[Array[Byte], Array[Byte]]` implementation.
 */
trait ThriftServerTrait extends TwitterServer {

  /** Add Framework Modules */
  addFrameworkModules(
    ExceptionManagerModule,
    StackTransformerModule,
    thriftResponseClassifierModule)

  /**
   * Default external Thrift port used as the [[Flag]] default value for [[thriftPortFlag]]. This
   * can be overridden to provide a different default programmatically when a flag value cannot be
   * passed. The format of this value is expected to be a String in the form of ":port".
   *
   * In general, users should prefer setting the [[thriftPortFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.thriftPortFlag]]
   */
  protected def defaultThriftPort: String = ":9999"

  /**
   * External Thrift port [[Flag]]. The default value is specified by [[defaultThriftPort]] which
   * can be overridden to provide a different default.
   *
   * @note the default value is ":9999" as defined by [[defaultThriftPort]].
   * @note the format of this flag is expected to be a String in the form of ":port".
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.defaultThriftPort]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val thriftPortFlag: Flag[String] =
    flag("thrift.port", defaultThriftPort, "External Thrift server port")

  /**
   * Default shutdown timeout used as the [[Flag]] default value for [[thriftShutdownTimeoutFlag]].
   * This represents the deadline for the closing of this server which can be overridden to provide
   * a different default programmatically when a flag value cannot be passed.
   *
   * In general, users should prefer setting the [[thriftShutdownTimeoutFlag]] [[Flag]] value.
   *
   * @note the value is used to denote a delta "from now", that is this value is applied as:
   *       `server.close(shutdownTimeoutDuration.fromNow())`
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.thriftShutdownTimeoutFlag]]
   * @see [[com.twitter.util.Closable.close(deadline: Time)]]
   * @see [[https://github.com/twitter/util/blob/b0a5d06269b9526b4408239ce1441b2a213dd0df/util-core/src/main/scala/com/twitter/util/Duration.scala#L436]]
   */
  protected def defaultThriftShutdownTimeout: Duration = 1.minute

  /**
   * Shutdown timeout [[Flag]]. The default value is specified by [[defaultThriftShutdownTimeout]]
   * which can be overridden to provide a different default.
   *
   * @note the default value is "1.minute" as defined by [[defaultThriftShutdownTimeout]].
   * @note the format of this flag is expected to be a String which is parsable into a [[com.twitter.util.Duration]].
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.defaultThriftShutdownTimeout]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val thriftShutdownTimeoutFlag: Flag[Duration] = flag(
    "thrift.shutdown.time",
    defaultThriftShutdownTimeout,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  /**
   * Default server name for the external Thrift interface used as the [[Flag]] default value for
   * [[thriftServerNameFlag]]. This can be overridden to provide a different default programmatically
   * when a flag value cannot be passed.
   *
   * In general, users should prefer setting the [[thriftServerNameFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.thriftServerNameFlag]]
   */
  protected def defaultThriftServerName: String = "thrift"

  /**
   * Server name for the external Thrift interface [[Flag]]. The default value is specified by
   * [[defaultThriftServerName]] which can be overridden to provide a different default.
   *
   * @note the default value is "thrift" as defined by [[defaultThriftServerName]].
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.defaultThriftServerName]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val thriftServerNameFlag: Flag[String] =
    flag("thrift.name", defaultThriftServerName, "Thrift server name")

  /**
   * Default server announcement String used as the [[Flag]] default value for [[thriftAnnounceFlag]].
   * This can be overridden to provide a different default programmatically when a flag value cannot
   * be passed. An empty String value is an indication to not perform any announcement of the server.
   *
   * In general, users should prefer setting the [[thriftAnnounceFlag]] [[Flag]] value.
   *
   * @see [[com.twitter.finagle.ListeningServer.announce(addr: String)]]
   */
  protected def defaultThriftAnnouncement: String = ThriftServerTrait.NoThriftAnnouncement

  /**
   * Server announcement String [[Flag]]. The default value is specified by [[defaultThriftAnnouncement]]
   * which can be overridden to provide a different default. Setting an empty String is an indication
   * to not perform any announcement of the server.
   *
   * @note the default value is "No Announcement" (empty String) as defined by [[defaultThriftAnnouncement]].
   * @see [[com.twitter.finagle.ListeningServer.announce(addr: String)]]
   * @see [[com.twitter.finatra.thrift.ThriftServerTrait.defaultThriftAnnouncement]]
   * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html#passing-flag-values-as-command-line-arguments]]
   */
  private val thriftAnnounceFlag: Flag[String] =
    flag[String](
      "thrift.announce",
      defaultThriftAnnouncement,
      "Address for announcing Thrift server. Empty string indicates no announcement."
    )

  /* Private Mutable State */

  private var thriftServer: ListeningServer = NullServer

  /* Abstract */

  /**
   * The Finagle `Service[Array[Byte], Array[Byte]]` to serve on the configured [[ListeningServer]].
   *
   * Users must override with an implementation to serve a `Service[Array[Byte], Array[Byte]]`.
   */
  protected def thriftService: Service[Array[Byte], Array[Byte]]

  /* Lifecycle */

  @Lifecycle
  override protected def postWarmup(): Unit = {
    super.postWarmup()

    thriftServer = build(
      thriftPortFlag(),
      configureThriftServer(
        ThriftMux.server
          .withLabel(thriftServerNameFlag())
          .withStatsReceiver(injector.instance[StatsReceiver].scope("srv"))
          .withResponseClassifier(injector.instance[ThriftResponseClassifier])
      )
    )

    onExit {
      Await.result(thriftServer.close(thriftShutdownTimeoutFlag().fromNow))
    }
    await(thriftServer)

    thriftAnnounceFlag() match {
      case ThriftServerTrait.NoThriftAnnouncement => // no-op
      case addr =>
        info(s"thrift server announced to $addr")
        thriftServer.announce(addr)
    }
    info(s"thrift server started on port: ${thriftPort.get}")
  }

  /* Overrides */

  override def thriftPort: Option[Int] = Some(PortUtils.getPort(thriftServer))

  /* Protected */

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[ThriftResponseClassifier]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[ThriftResponseClassifier]] implementation.
   */
  protected def thriftResponseClassifierModule: Module = ThriftResponseClassifierModule

  /**
   * This method allows for further configuration of the thrift server for parameters not exposed by
   * this trait or for overriding defaults provided herein, e.g.,
   *
   * override def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
   *   server
   *     .withMaxReusableBufferSize(...)
   * }
   *
   * @param server - the [[com.twitter.finagle.ThriftMux.Server]] to configure.
   * @return a configured ThriftMux.Server.
   */
  protected def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
    server
  }

  /**
   * Construct a [[com.twitter.finagle.ListeningServer]] from the given String addr
   * and configured [[ThriftMux.Server]] stack.
   *
   * @param addr the [[String]] address to bind the resultant [[ListeningServer]].
   * @param server the configured [[ThriftMux.Server]] stack.
   * @return a constructed [[ListeningServer]].
   */
  protected[thrift] def build(addr: String, server: ThriftMux.Server): ListeningServer = {
    server.serve(addr, this.thriftService)
  }
}

/**
 * A Finagle server which exposes an external Thrift interface implemented by a
 * `Service[Array[Byte], Array[Byte]]` configured via a [[ThriftRouter]]. This trait is
 * intended for use from Scala or with generated Scala code.
 *
 * @note Java users are encouraged to use [[AbstractThriftServer]] instead.
 */
trait ThriftServer extends ThriftServerTrait {

  /**
   * Configuration of the `Service[Array[Byte], Array[Byte]]` to serve on the [[ListeningServer]]
   * is defined by configuring the [[ThriftRouter]] and not by implementation of this method,
   * thus this method overridden to be final and set to a `NilService`.
   */
  protected final def thriftService: Service[Array[Byte], Array[Byte]] = NilService

  /** Serve the `Service[Array[Byte], Array[Byte]]` from the configured [[ThriftRouter]]. */
  override protected[thrift] final def build(addr: String, server: ThriftMux.Server): ListeningServer = {
    val router = injector.instance[ThriftRouter]
    server.serveIface(addr, router.thriftService)
  }

  /* Abstract */

  /**
   * Users MUST provide an implementation to configure the provided [[ThriftRouter]]. The [[ThriftRouter]]
   * exposes a DSL which results in a configured Finagle `Service[-Req, +Rep]` to serve on
   * the [[ListeningServer]].
   *
   * @param router the [[ThriftRouter]] to configure.
   */
  protected def configureThrift(router: ThriftRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    configureThrift(injector.instance[ThriftRouter])
  }
}

/**
 * A Finagle server which exposes an external Thrift interface implemented by a
 * `Service[Array[Byte], Array[Byte]]` configured via a [[JavaThriftRouter]]. This abstract class is
 * intended for use from Java or with generated Java code.
 *
 * @note Scala users are encouraged to use [[ThriftServer]] instead.
 */
abstract class AbstractThriftServer extends ThriftServerTrait {

  /** This Server returns a [[JavaThriftRouter]] configured `Service[Array[Byte], Array[Byte]]` */
  protected final def thriftService: Service[Array[Byte], Array[Byte]] = {
    val router = injector.instance[JavaThriftRouter]
    registerService(
      configureService(router.service))
  }

  /* Abstract */

  /**
   * Users MUST provide an implementation to configure the provided [[JavaThriftRouter]]. The [[JavaThriftRouter]]
   * exposes a DSL which results in a configured Finagle `Service[-Req, +Rep]` to serve on the [[ListeningServer]].
   *
   * @param router the [[JavaThriftRouter]] to configure.
   */
  protected def configureThrift(router: JavaThriftRouter): Unit

  /* Protected */

  /**
   * Override to provide further configuration to the `Service[Array[Byte], Array[Byte]]` served.
   * For example, to add "global" filters over the resultant `Service[Array[Byte], Array[Byte]]`.
   *
   * E.g.
   *
   * {{{
   *   override protected def configureService(
   *     service: Service[Array[Byte], Array[Byte]]
   *   ): Service[Array[Byte], Array[Byte]] = {
   *      injector.instance[MyGreatServiceFilter].andThen(service)
   *   }
   * }}}
   */
  protected def configureService(
    service: Service[Array[Byte], Array[Byte]]
  ): Service[Array[Byte], Array[Byte]] = service

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    configureThrift(injector.instance[JavaThriftRouter])
  }

  /* Private */

  /** The service may be filtered and thus we want to fully capture in the registry the resultant service */
  private[this] def registerService[Req, Rep](
    service: Service[Req, Rep]
  ): Service[Req, Rep] = {
    injector
      .instance[LibraryRegistry]
      .withSection("thrift")
      .put("service", service.toString)
    service
  }
}
