package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.service.Filterable
import com.twitter.finagle.thrift.{ClientId, ServiceIfaceBuilder}
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.filters.DarkTrafficFilter
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import scala.reflect.ClassTag

abstract class DarkTrafficFilterModule[ServiceIface <: Filterable[ServiceIface]: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface]
) extends TwitterModule {

  private val destFlag =
    flag[String]("thrift.dark.service.dest", "Resolvable name/dest of dark traffic service")
  private val clientIdFlag =
    flag("thrift.dark.service.clientId", "", "Thrift client id to the dark traffic service")

  /**
   * Name of dark service client for use in metrics.
   */
  val label: String = "service"

  /**
   * Forward the dark request after the service has processed the request
   * instead of concurrently.
   */
  val forwardAfterService: Boolean = true

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  def enableSampling(injector: Injector): ThriftRequest[_] => Boolean

  /**
   * Override to specify further configuration of the underlying Finagle [[ThriftMux.Client]].
   *
   * @param injector  the [[com.twitter.inject.Injector]] for use in configuring the underlying client.
   * @param client    the default configured [[ThriftMux.Client]].
   *
   * @return a configured instance of the [[ThriftMux.Client]]
   */
  protected def configureThriftMuxClient(
    injector: Injector,
    client: ThriftMux.Client
  ): ThriftMux.Client = client

  @Provides
  @Singleton
  @DarkTrafficFilterType
  final def providesDarkTrafficFilter(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): ThriftFilter = {
    destFlag.get match {
      case Some(dest) =>
        val clientStatsReceiver =
          statsReceiver.scope("clnt", "dark_traffic_filter")
        val clientId = ClientId(clientIdFlag())

        val service =
          configureThriftMuxClient(
            injector,
            defaultThriftMuxClient(clientId, clientStatsReceiver)
          ).newServiceIface[ServiceIface](dest, label)

        new DarkTrafficFilter[ServiceIface](
          service,
          enableSampling(injector),
          forwardAfterService,
          statsReceiver
        )
      case _ =>
        ThriftFilter.Identity
    }
  }

  /* Private */

  private[this] def defaultThriftMuxClient(
    clientId: ClientId,
    statsReceiver: StatsReceiver
  ): ThriftMux.Client = {

    ThriftMux.client
      .withStatsReceiver(statsReceiver)
      .withClientId(clientId)
      .withPerEndpointStats
  }
}
