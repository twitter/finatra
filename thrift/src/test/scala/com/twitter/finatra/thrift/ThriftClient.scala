package com.twitter.finatra.thrift

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.transport.Transport
import com.twitter.finatra.thrift.tests.doeverything.TLSConfigurator
import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.server.PortUtils._
import com.twitter.inject.server.{PortUtils, Ports}
import scala.reflect.ClassTag

trait ThriftClient { self: EmbeddedApp =>

  def twitterServer: Ports

  def thriftPortFlag: String = "thrift.port"

  /* Overrides */

  override protected def logAppStartup() {
    self.logAppStartup()
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
      ThriftMux.Client()
        .configured(Stats(NullStatsReceiver))
        .configured(
          Transport.TLSClientEngine(
            Some(_ => TLSConfigurator.clientTLSEngine)
          )
        )

    val client = {
      if (clientId != null) {
        baseThriftClient.withClientId(ClientId(clientId))
      } else baseThriftClient
    }

    client.newIface[T](loopbackAddressForPort(thriftExternalPort))
  }
}
