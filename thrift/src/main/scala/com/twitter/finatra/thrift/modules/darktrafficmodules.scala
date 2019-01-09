package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.service.{Filterable, ReqRepServicePerEndpointBuilder}
import com.twitter.finagle.thrift.{ClientId, ServiceIfaceBuilder}
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.filters.{DarkTrafficFilter, JavaDarkTrafficFilter}
import com.twitter.inject.{Injector, Logging, TwitterModule}
import javax.inject.Singleton
import scala.reflect.ClassTag

private[modules] abstract class AbstractDarkTrafficFilterModule
  extends TwitterModule
    with Logging {

  /** Name of the flag which captures the destination to which to send dark traffic. */
  protected def destFlagName: String = "thrift.dark.service.dest"
  protected[this] val destFlag: Flag[String] =
    flag[String](destFlagName, "Resolvable name/dest of dark traffic service")

  /** Name of the flag which captures the value for the Thrift ClientId to pass to the dark service */
  protected def clientIdFlagName: String = "thrift.dark.service.clientId"
  protected def defaultClientId: String = ""
  protected[this] val clientIdFlag: Flag[String] =
    flag(clientIdFlagName, defaultClientId, "Thrift client id to the dark traffic service")

  /** Name of dark service client for use in metrics. */
  protected def label: String = "service"

  /**
   * Forward the dark request after the service has processed the request
   * instead of concurrently.
   */
  protected def forwardAfterService: Boolean = true

  protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): Filter.TypeAgnostic

  protected def configureThriftMuxClient(
    injector: Injector,
    client: ThriftMux.Client
  ): ThriftMux.Client = client

  protected[this] def defaultThriftMuxClient(
    clientId: ClientId,
    statsReceiver: StatsReceiver
  ): ThriftMux.Client = {
    ThriftMux.client
      .withStatsReceiver(statsReceiver)
      .withClientId(clientId)
      .withPerEndpointStats
  }

  @Provides
  @Singleton
  @DarkTrafficFilterType
  final def providesDarkTrafficFilter(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Filter.TypeAgnostic = {
    destFlag.get match {
      case Some(dest) if dest.nonEmpty =>
        val clientStatsReceiver =
          statsReceiver.scope("clnt", "dark_traffic_filter")
        val clientId = ClientId(clientIdFlag())

        val configuredClient =
          configureThriftMuxClient(
            injector,
            defaultThriftMuxClient(clientId, clientStatsReceiver)
          )

        newFilter(dest, configuredClient, injector, statsReceiver)

      case _ =>
        Filter.TypeAgnostic.Identity
    }
  }
}

/**
 * A [[TwitterModule]] which configures and binds a [[DarkTrafficFilter]] to the object graph.
 *
 * @note This is only applicable in Scala as it uses generated Scala classes and expects to configure
 *       the [[DarkTrafficFilter]] over a [[com.twitter.finagle.Service]] that is generated from
 *       Finagle via generated Scala code. Users of generated Java code should use the
 *       [[JavaDarkTrafficFilterModule]].
 */
abstract class DarkTrafficFilterModule[ServiceIface <: Filterable[ServiceIface]: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface]
) extends AbstractDarkTrafficFilterModule {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  protected def enableSampling(injector: Injector): Any => Boolean

  protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): Filter.TypeAgnostic = {
    new DarkTrafficFilter[ServiceIface](
      client.newServiceIface[ServiceIface](dest, label),
      enableSampling(injector),
      forwardAfterService,
      stats,
      lookupByMethod = false
    )
  }
}

abstract class ReqRepDarkTrafficFilterModule[MethodIface <: Filterable[MethodIface]: ClassTag](
  implicit serviceBuilder: ReqRepServicePerEndpointBuilder[MethodIface]
) extends AbstractDarkTrafficFilterModule {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  protected def enableSampling(injector: Injector): Any => Boolean

  protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): DarkTrafficFilter[MethodIface] = {
    new DarkTrafficFilter[MethodIface](
      client.servicePerEndpoint[MethodIface](dest, label),
      enableSampling(injector),
      forwardAfterService,
      stats,
      lookupByMethod = true
    )
  }
}

/**
 * A [[TwitterModule]] which configures and binds a [[JavaDarkTrafficFilter]] to the object graph.
 *
 * @note This is only applicable with code that uses generated Java classes as it expects to configure
 *       the [[JavaDarkTrafficFilter]] over a [[com.twitter.finagle.Service]] that is generated from
 *       Finagle via generated Java code. Users of generated Scala code should use the
 *       [[DarkTrafficFilterModule]].
 */
abstract class JavaDarkTrafficFilterModule extends AbstractDarkTrafficFilterModule {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  protected def enableSampling(injector: Injector): com.twitter.util.Function[Array[Byte], Boolean]

  protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): Filter.TypeAgnostic = {
    new JavaDarkTrafficFilter(
      client.newService(dest, label),
      enableSampling(injector),
      forwardAfterService,
      stats
    )
  }
}
