package com.twitter.finatra.thrift

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.server.PortUtils._
import com.twitter.inject.server.{EmbeddedTwitterServer, PortUtils, Ports}
import scala.reflect.ClassTag

trait ThriftClient { self: EmbeddedTwitterServer =>

  def twitterServer: Ports

  def thriftPortFlag: String = "thrift.port"

  /* Overrides */

  override protected def logStartup() {
    self.logStartup()
    info(s"ExternalThrift -> thrift://$externalThriftHostAndPort\n")
  }

  override protected def combineArgs(): Array[String] = {
    s"-$thriftPortFlag=${PortUtils.ephemeralLoopback}" +: self.combineArgs
  }

  /* Public */

  lazy val externalThriftHostAndPort = PortUtils.loopbackAddressForPort(thriftExternalPort)

  def thriftExternalPort: Int = {
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
