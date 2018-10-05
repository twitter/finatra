package com.twitter.finatra.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.{ListeningServer, NullServer, ThriftMux}
import com.twitter.finatra.thrift.modules.ExceptionManagerModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.{Await, Duration}

/** AbstractThriftServer for usage from Java */
abstract class AbstractThriftServer extends ThriftServer

private object ThriftServer {
  /**
   * Sentinel used to indicate no announcement.
   */
  val NoThriftAnnouncement: String = ""
}

trait ThriftServer extends TwitterServer {

  addFrameworkModules(ExceptionManagerModule)

  protected def defaultThriftPort: String = ":9999"
  private val thriftPortFlag =
    flag("thrift.port", defaultThriftPort, "External Thrift server port")

  protected def defaultThriftShutdownTimeout: Duration = 1.minute
  private val thriftShutdownTimeoutFlag = flag(
    "thrift.shutdown.time",
    defaultThriftShutdownTimeout,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  protected def defaultThriftServerName: String = "thrift"
  private val thriftServerNameFlag =
    flag("thrift.name", defaultThriftServerName, "Thrift server name")

  protected def defaultThriftAnnouncement: String = ThriftServer.NoThriftAnnouncement
  private val thriftAnnounceFlag =
    flag[String]("thrift.announce", defaultThriftAnnouncement,
      "Address for announcing Thrift server. Empty string indicates no announcement.")

  /* Private Mutable State */

  private var thriftServer: ListeningServer = NullServer

  /* Abstract */

  protected def configureThrift(router: ThriftRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    val router = injector.instance[ThriftRouter]
    router.serviceName(name)
    configureThrift(router)
  }

  @Lifecycle
  override protected def postWarmup(): Unit = {
    super.postWarmup()

    val router = injector.instance[ThriftRouter]
    val thriftServerBuilder =
      configureThriftServer(
        ThriftMux.server
          .withLabel(thriftServerNameFlag())
      )

    thriftServer = router.services.service
      .map { service =>
        // if we have a built Service[-Req, +Rep] we serve it
        thriftServerBuilder.serve(thriftPortFlag(), service)
      }
      .getOrElse {
        // otherwise we serve from a ServiceIface
        thriftServerBuilder.serveIface(thriftPortFlag(), router.services.serviceIface)
      }
    onExit {
      Await.result(thriftServer.close(thriftShutdownTimeoutFlag().fromNow))
    }
    await(thriftServer)

    thriftAnnounceFlag() match {
      case ThriftServer.NoThriftAnnouncement => // no-op
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
   * This method allows for further configuration of the thrift server for parameters not exposed by
   * this trait or for overriding defaults provided herein, e.g.,
   *
   * override def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
   *   server
   *     .withResponseClassifier(...)
   *     .withMaxReusableBufferSize(...)
   * }
   *
   * @param server - the [[com.twitter.finagle.ThriftMux.Server]] to configure.
   * @return a configured ThriftMux.Server.
   */
  protected def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
    server
  }
}
