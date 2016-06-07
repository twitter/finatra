package com.twitter.finatra.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.{Await, Future, Time}

trait ThriftServer extends TwitterServer {

  addFrameworkModule(
    Slf4jBridgeModule)

  protected def defaultFinatraThriftPort: String = ":9999"
  private val thriftPortFlag = flag("thrift.port", defaultFinatraThriftPort, "External Thrift server port")

  protected def defaultThriftShutdownTimeout = 1.minute
  private val thriftShutdownTimeoutFlag = flag("thrift.shutdown.time", defaultThriftShutdownTimeout, "Maximum amount of time to wait for pending requests to complete on shutdown")

  protected def defaultThriftServerName: String = "thrift"
  private val thriftServerNameFlag = flag("thrift.name", defaultThriftServerName, "Thrift server name")

  private val thriftAnnounceFlag = flag[String]("thrift.announce", "Address for announcing Thrift server")

  /* Private Mutable State */
  private var thriftServer: ListeningServer = _

  /* Abstract */

  protected def configureThrift(router: ThriftRouter): Unit

  // TODO: move upstream to inject.App; requires inject.TwitterServer#run to be
  // renamed (to handle[T]) and then this should replace inject.App#appMain().
  /**
   * Application logic to run after the server has warmed up and bound to its port(s).
   */
  protected def run(): Unit = {}

  /* Lifecycle */

  override protected def postStartup(): Unit = {
    super.postStartup()
    val router = injector.instance[ThriftRouter]
    router.serviceName(name)
    configureThrift(router)
  }

  override protected def postWarmup() {
    super.postWarmup()

    val router = injector.instance[ThriftRouter]
    val thriftServerBuilder =
      configureThriftServer(
        ThriftMux.server
          .withLabel(thriftServerNameFlag()))

    thriftServer =
      thriftServerBuilder.serveIface(thriftPortFlag(), router.filteredService)
    onExit {
      Await.result(
        close(thriftServer, thriftShutdownTimeoutFlag().fromNow))
    }
    for (addr <- thriftAnnounceFlag.get) thriftServer.announce(addr)
    info("Thrift server started on port: " + thriftPort.get)
  }

  override def waitForServer() {
    Await.ready(thriftServer)
  }

  /* Overrides */

  override protected def failfastOnFlagsNotParsed = true

  override def thriftPort = Option(thriftServer) map PortUtils.getPort

  override final def appMain(): Unit = { run() }

  /* Protected */

  protected def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
    server
  }

  /* Private */

  private def close(server: ListeningServer, deadline: Time) = {
    if (server != null)
      server.close(deadline)
    else
      Future.Unit
  }
}
