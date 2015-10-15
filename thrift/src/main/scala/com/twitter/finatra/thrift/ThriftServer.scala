package com.twitter.finatra.thrift

import com.twitter.conversions.time._
import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.Await

trait ThriftServer extends TwitterServer {

  private val thriftPortFlag = flag("thrift.port", ":9999", "External Thrift server port")
  private val thriftShutdownTimeout = flag("thrift.shutdown.time", 1.minute, "Maximum amount of time to wait for pending requests to complete on shutdown")

  /* Private Mutable State */
  private var thriftServer: ListeningServer = _

  /* Abstract */

  protected def configureThrift(router: ThriftRouter)

  /* Lifecycle */

  override def postWarmup() {
    super.postWarmup()

    val router = injector.instance[ThriftRouter]
    router.serviceName(name)
    configureThrift(router)
    thriftServer = ThriftMux.serveIface(thriftPortFlag(), router.filteredService)
    info("Thrift server started on port: " + thriftPort.get)
  }

  onExit {
    Await.result(
      thriftServer.close(thriftShutdownTimeout().fromNow))
  }

  /* Overrides */

  override def thriftPort = Option(thriftServer) map PortUtils.getPort
}
