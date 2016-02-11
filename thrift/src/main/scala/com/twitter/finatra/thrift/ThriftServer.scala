package com.twitter.finatra.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.{Await, Future, Time}

trait ThriftServer extends TwitterServer {

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

  /* Lifecycle */

  override def postWarmup() {
    super.postWarmup()

    val router = injector.instance[ThriftRouter]
    router.serviceName(name)
    configureThrift(router)
    thriftServer = ThriftMux.server
      .withLabel(thriftServerNameFlag())
      .serveIface(thriftPortFlag(), router.filteredService)
    onExit {
      Await.result(
        close(thriftServer, thriftShutdownTimeoutFlag().fromNow))
    }
    for (addr <- thriftAnnounceFlag.get) thriftServer.announce(addr)
    info("Thrift server started on port: " + thriftPort.get)
  }

  /* Overrides */

  override protected def failfastOnFlagsNotParsed = true

  override def thriftPort = Option(thriftServer) map PortUtils.getPort

  /* Protected */

  /* Private */

  private def close(server: ListeningServer, deadline: Time) = {
    if (server != null)
      server.close(deadline)
    else
      Future.Unit
  }
}
