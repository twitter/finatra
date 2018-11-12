package com.twitter.finatra.thrift

import com.google.inject.Module
import com.twitter.app.Flag
import com.twitter.conversions.time._
import com.twitter.finagle.service.NilService
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{ListeningServer, NullServer, Service, ThriftMux}
import com.twitter.finatra.thrift.modules.{ExceptionManagerModule, ThriftResponseClassifierModule}
import com.twitter.finatra.thrift.response.ThriftResponseClassifier
import com.twitter.finatra.thrift.routing.{JavaThriftRouter, ThriftRouter}
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.{Await, Duration}

private object ThriftServerTrait {
  /**
   * Sentinel used to indicate no announcement.
   */
  val NoThriftAnnouncement: String = ""
}

/**
 * A Basic ThriftServer. To implement, override
 * {{{
 *   protected def service: Service[Array[Byte], Array[Byte]]
 * }}}
 *
 * with your `Service[Array[Byte], Array[Byte]]` implementation.
 */
trait ThriftServerTrait extends TwitterServer {

  /** Add Framework Modules */
  addFrameworkModules(
    ExceptionManagerModule,
    thriftResponseClassifierModule)

  /** Thrift Port */
  protected def defaultThriftPort: String = ":9999"
  private val thriftPortFlag: Flag[String] =
    flag("thrift.port", defaultThriftPort, "External Thrift server port")

  /** Shutdown Timeout */
  protected def defaultThriftShutdownTimeout: Duration = 1.minute
  private val thriftShutdownTimeoutFlag: Flag[Duration] = flag(
    "thrift.shutdown.time",
    defaultThriftShutdownTimeout,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  /** Server Name */
  protected def defaultThriftServerName: String = "thrift"
  private val thriftServerNameFlag: Flag[String] =
    flag("thrift.name", defaultThriftServerName, "Thrift server name")

  /** Server Announcement */
  protected def defaultThriftAnnouncement: String = ThriftServerTrait.NoThriftAnnouncement
  private val thriftAnnounceFlag: Flag[String] =
    flag[String]("thrift.announce", defaultThriftAnnouncement,
      "Address for announcing Thrift server. Empty string indicates no announcement.")

  /* Private Mutable State */

  private var thriftServer: ListeningServer = NullServer

  /* Abstract */

  /** Override with an implementation to serve a Thrift Service */
  protected def service: Service[Array[Byte], Array[Byte]]

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
          .withResponseClassifier(injector.instance[ThriftResponseClassifier]))
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

  override def thriftPort: Option[Int] = Option(thriftServer).map(PortUtils.getPort)

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
  protected def build(addr: String, server: ThriftMux.Server): ListeningServer = {
    server.serve(addr, this.service)
  }
}

/** ThriftServer for usage from Scala */
trait ThriftServer extends ThriftServerTrait {

  /** This Server does not return a `Service[Array[Byte], Array[Byte]]` */
  final protected val service: Service[Array[Byte], Array[Byte]] = NilService

  override final protected def build(addr: String, server: ThriftMux.Server): ListeningServer = {
    val router = injector.instance[ThriftRouter]
    server.serveIface(addr, router.thriftService)
  }

  /* Abstract */

  protected def configureThrift(router: ThriftRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    configureThrift(injector.instance[ThriftRouter])
  }
}

/** AbstractThriftServer for usage from Java or with generated Java code */
abstract class AbstractThriftServer extends ThriftServerTrait {
  final protected def service: Service[Array[Byte], Array[Byte]] = {
    val router = injector.instance[JavaThriftRouter]
    router.service
  }

  /* Abstract */

  protected def configureThrift(router: JavaThriftRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    configureThrift(injector.instance[JavaThriftRouter])
  }
}
