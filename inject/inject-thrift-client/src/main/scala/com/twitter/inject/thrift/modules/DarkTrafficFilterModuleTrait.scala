package com.twitter.inject.thrift.modules

import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.inject.Injector
import javax.inject.Singleton

private[modules] trait DarkTrafficFilterModuleTrait
  extends ThriftClientModuleTrait {

  /** Name of the flag which captures the destination to which to send dark traffic. */
  protected def destFlagName: String = "thrift.dark.service.dest"
  protected[this] val destFlag: Flag[String] =
    flag[String](destFlagName, "Resolvable name/dest of dark traffic service")

  /** Name of the flag which captures the value for the Thrift ClientId to pass to the dark service */
  protected def clientIdFlagName: String = "thrift.dark.service.clientId"
  protected def defaultClientId: String = ""
  protected[this] val clientIdFlag: Flag[String] =
    flag(clientIdFlagName, defaultClientId, "Thrift client id to the dark traffic service")

  override protected final def clientId(injector: Injector): ClientId = ClientId(clientIdFlag())

  override protected def initialClientConfiguration(
    injector: Injector,
    client: ThriftMux.Client,
    statsReceiver: StatsReceiver
  ): ThriftMux.Client = super.initialClientConfiguration(injector, client, statsReceiver)
    .withPerEndpointStats

  /** Name of dark service client for use in metrics. */
  def label: String = "service"

  def dest: String = destFlag()

  /**
   * Forward the dark request after the service has processed the request
   * instead of concurrently.
   */
  protected def forwardAfterService: Boolean = true

  override protected final def scopeStatsReceiver(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): StatsReceiver = statsReceiver.scope("clnt", "dark_traffic_filter")

  protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): Filter.TypeAgnostic

  @Provides
  @Singleton
  @DarkTrafficFilterType
  def providesDarkTrafficFilter(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Filter.TypeAgnostic = {
    destFlag.get match {
      case Some(dest) if dest.nonEmpty =>
        val client = newClient(injector, statsReceiver)
        newFilter(dest, client, injector, statsReceiver)

      case _ =>
        Filter.TypeAgnostic.Identity
    }
  }
}
