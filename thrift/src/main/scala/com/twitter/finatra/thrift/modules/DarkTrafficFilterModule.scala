package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{ClientId, ServiceIfaceBuilder}
import com.twitter.finagle.{Thrift, ThriftMux}
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.filters.DarkTrafficFilter
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.inject.{RootMonitor, TwitterModule}
import com.twitter.util.Monitor
import javax.inject.Singleton
import org.joda.time.Duration
import scala.reflect.ClassTag

object DarkTrafficFilterModule {
  val MaxDuration = Duration.millis(Long.MaxValue)
}

abstract class DarkTrafficFilterModule[ServiceIface: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface])
  extends TwitterModule {

  private val destFlag = flag[String]("thrift.dark.service.dest", "Resolvable name/dest of dark traffic service")
  private val clientIdFlag = flag("thrift.dark.service.clientId", "", "Thrift client id to the dark traffic service")

  /**
   * Name of dark service client for use in metrics.
   */
  val label: String = "service"

  /**
   * Enable thrift mux for this connection.
   *
   * Note: Both server and client must have mux enabled otherwise
   * a nondescript ChannelClosedException will be seen.
   *
   * What is ThriftMux?
   * http://twitter.github.io/finagle/guide/FAQ.html?highlight=thriftmux#what-is-thriftmux
   */
  protected val mux: Boolean = true

  /**
   * Forward the dark request after the service has processed the request
   * instead of concurrently.
   */
  val forwardAfterService: Boolean = true

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  def enableSampling: ThriftRequest[_] => Boolean

  /* Client defaults */
  protected def monitor: Monitor = RootMonitor

  @Provides
  @Singleton
  @DarkTrafficFilterType
  final def providesDarkTrafficFilter(
   statsReceiver: StatsReceiver
  ): ThriftFilter = {
    destFlag.get match {
      case Some(dest) =>
        val clientStatsReceiver = statsReceiver.scope("clnt", "dark_traffic_filter")
        val clientId = ClientId(clientIdFlag())

        val service = newServiceIface(
          dest,
          clientId,
          clientStatsReceiver)

        new DarkTrafficFilter[ServiceIface](
          service,
          enableSampling,
          forwardAfterService,
          statsReceiver)

      case _ => ThriftFilter.Identity
    }
  }

  /* Private */

  private def newServiceIface(
    dest: String,
    clientId: ClientId,
    statsReceiver: StatsReceiver): ServiceIface = {

    if (mux) {
      ThriftMux.client
        .withStatsReceiver(statsReceiver)
        .withClientId(clientId)
        .withMonitor(monitor)
        .newServiceIface[ServiceIface](dest, label)
    } else {
      Thrift.client
        .withStatsReceiver(statsReceiver)
        .withClientId(clientId)
        .withMonitor(monitor)
        .newServiceIface[ServiceIface](dest, label)
    }
  }
}
