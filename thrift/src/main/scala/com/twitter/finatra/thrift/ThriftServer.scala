package com.twitter.finatra.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.{NullServer, ListeningServer, ThriftMux}
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.Await

/** AbstractThriftServer for usage from Java */
abstract class AbstractThriftServer extends ThriftServer

trait ThriftServer extends TwitterServer {

  protected def defaultFinatraThriftPort: String = ":9999"
  private val thriftPortFlag = flag("thrift.port", defaultFinatraThriftPort, "External Thrift server port")

  protected def defaultThriftShutdownTimeout = 1.minute
  private val thriftShutdownTimeoutFlag = flag("thrift.shutdown.time", defaultThriftShutdownTimeout, "Maximum amount of time to wait for pending requests to complete on shutdown")

  protected def defaultThriftServerName: String = "thrift"
  private val thriftServerNameFlag = flag("thrift.name", defaultThriftServerName, "Thrift server name")

  private val thriftAnnounceFlag = flag[String]("thrift.announce", "Address for announcing Thrift server")

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
          .withLabel(thriftServerNameFlag()))

    thriftServer = router.services.service.map { service =>
      thriftServerBuilder.serve(thriftPortFlag(), service)
    }.getOrElse {
      thriftServerBuilder.serveIface(thriftPortFlag(), router.services.serviceIface)
    }
    onExit {
      Await.result(
        thriftServer.close(thriftShutdownTimeoutFlag().fromNow))
    }
    await(thriftServer)
    for (addr <- thriftAnnounceFlag.get) thriftServer.announce(addr)
    info("thrift server started on port: " + thriftPort.get)
  }

  /* Overrides */

  override def thriftPort: Option[Int] = Option(thriftServer) map PortUtils.getPort

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
