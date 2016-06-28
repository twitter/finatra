package com.twitter.inject.thrift.modules

import com.github.nscala_time.time
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.factory.TimeoutFactory
import com.twitter.finagle.param.Stats
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import javax.inject.Singleton
import scala.reflect.ClassTag

@deprecated("Use the com.twitter.inject.thrift.modules.FilteredThriftClientModule", "2016-06-23")
abstract class ThriftClientModule[T: ClassTag]
  extends TwitterModule
  with time.Implicits {

  /**
   * Name of client for use in metrics
   */
  val label: String

  /**
   * Destination of client
   */
  val dest: String

  /**
   * Enable thrift mux for this connection.
   *
   * Note: Both server and client must have mux enabled otherwise
   * a nondescript ChannelClosedException will be seen.
   *
   * What is ThriftMux?
   * http://twitter.github.io/finagle/guide/FAQ.html?highlight=thriftmux#what-is-thriftmux
   */
  def mux: Boolean = true

  def requestTimeout: Duration = Duration.Top

  def connectTimeout: Duration = Duration.Top

  @Singleton
  @Provides
  def providesClient(clientId: ClientId, statsReceiver: StatsReceiver): T = {
    val labelAndDest = s"$label=$dest"

    if (mux) {
      ThriftMux.client.
        configured(TimeoutFilter.Param(requestTimeout)).
        configured(TimeoutFactory.Param(connectTimeout)).
        configured(Stats(statsReceiver.scope("clnt"))).
        withClientId(clientId).
        newIface[T](labelAndDest)
    }
    else {
      Thrift.client.
        configured(TimeoutFilter.Param(requestTimeout)).
        configured(TimeoutFactory.Param(connectTimeout)).
        configured(Stats(statsReceiver.scope("clnt"))).
        withClientId(clientId).
        newIface[T](labelAndDest)
    }
  }
}
