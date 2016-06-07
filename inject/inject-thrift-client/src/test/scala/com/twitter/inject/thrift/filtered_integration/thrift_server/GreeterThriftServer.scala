package com.twitter.inject.thrift.filtered_integration.thrift_server

import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.Await

class GreeterThriftServer extends TwitterServer {

  private val thriftPortFlag = flag("thrift.port", ":0", "External Thrift server port")

  /* Private Mutable State */
  private var thriftServer: ListeningServer = _

  /* Lifecycle */

  override def postWarmup() {
    super.postWarmup()

    thriftServer = ThriftMux.server.serveIface(
      thriftPortFlag(),
      injector.instance[GreeterImpl])

    info("Thrift server started on port: " + thriftPort.get)
  }

  onExit {
    Await.result(
      thriftServer.close())
  }

  /* Overrides */

  override def thriftPort = Option(thriftServer) map PortUtils.getPort

}
