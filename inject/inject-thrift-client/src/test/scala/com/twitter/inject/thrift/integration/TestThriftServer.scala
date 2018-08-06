package com.twitter.inject.thrift.integration

import com.twitter.conversions.time._
import com.twitter.finagle.{ListeningServer, ThriftMux}
import com.twitter.finagle.thrift.ThriftService
import com.twitter.inject.server.{PortUtils, TwitterServer}
import com.twitter.util.Await

class TestThriftServer(service: ThriftService) extends TwitterServer {
  private val thriftPortFlag = flag("thrift.port", ":0", "External Thrift server port")
  private val thriftShutdownTimeout = flag(
    "thrift.shutdown.time",
    1.minute,
    "Maximum amount of time to wait for pending requests to complete on shutdown"
  )

  /* Private Mutable State */
  private var thriftServer: ListeningServer = _

  /* Lifecycle */
  override def postWarmup(): Unit = {
    super.postWarmup()
    thriftServer = ThriftMux.server.serveIface(thriftPortFlag(), service)
    info("Thrift server started on port: " + thriftPort.get)
  }

  onExit {
    Await.result(thriftServer.close(thriftShutdownTimeout().fromNow))
  }

  /* Overrides */
  override def thriftPort: Option[Int] = Option(thriftServer) map PortUtils.getPort
}
