package com.twitter.inject.thrift.integration

import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.scrooge.ThriftService
import com.twitter.util.Await

class TestThriftServer(service: ThriftService) extends TwitterServer {
  private val thriftPortFlag = flag("thrift.port", ":0", "External Thrift server port")
  /* Private Mutable State */
  private var thriftServer: ListeningServer = _

  /* Lifecycle */
  override def postWarmup() {
    super.postWarmup()
    thriftServer = ThriftMux.server.serveIface(thriftPortFlag(), service)
    info("Thrift server started on port: " + thriftPort.get)
  }

  onExit {
    Await.result(thriftServer.close())
  }

  /* Overrides */
  override def thriftPort = Option(thriftServer) map PortUtils.getPort
}
