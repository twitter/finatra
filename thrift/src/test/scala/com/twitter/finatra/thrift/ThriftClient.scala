package com.twitter.finatra.thrift

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.server.PortUtils._
import com.twitter.inject.server.{PortUtils, Ports}
import scala.reflect.ClassTag

trait ThriftClient { self: EmbeddedApp =>

  def twitterServer: Ports

  override protected def combineArgs(): Array[String] = {
    ("-thrift.port=" + PortUtils.ephemeralLoopback) +: self.combineArgs
  }

  def thriftExternalPort = {
    self.start()
    twitterServer.thriftPort.get
  }

  def thriftClient[T: ClassTag](clientId: String = null): T = {
    val baseThriftClient =
      ThriftMux.Client().
        configured(Stats(NullStatsReceiver))

    val client = {
      if (clientId != null) {
        baseThriftClient.withClientId(ClientId(clientId))
      } else baseThriftClient
    }

    client.newIface[T](loopbackAddressForPort(thriftExternalPort))
  }
}
