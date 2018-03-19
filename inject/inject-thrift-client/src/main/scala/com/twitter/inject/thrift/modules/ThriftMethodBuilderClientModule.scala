package com.twitter.inject.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.thrift.service.{Filterable, MethodPerEndpointBuilder, ServicePerEndpointBuilder}
import com.twitter.finagle.{ThriftMux, thriftmux}
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Await, Duration, Monitor, NullMonitor}
import javax.inject.Singleton

/**
 * A [[TwitterModule]] which allows for configuration of a `ThriftMux` client. The client interface
 * can be expressed as a `service-per-endpoint` or a `MethodPerEndpoint`.
 *
 * Provides bindings for a Scrooge-generated `service-per-endpoint` and `MethodPerEndpoint`. The
 * `MethodPerEndpoint` is constructed via the [[MethodPerEndpointBuilder]] and is thus implemented
 * as a thin wrapper over the `service-per-endpoint`.
 *
 * This [[TwitterModule]] allows users to configure and filter a Scrooge-generated `service-per-endpoint`
 * per-method which can then be used directly or can be wrapped by a `MethodPerEndpoint`.
 *
 * @note When applying filters, filter order matters. The order in which filters are applied
 *       is the order in which requests will flow through to the service and the opposite of the
 *       order in which responses return. See the [[ThriftMethodBuilderFactory]] for more information.
 *
 * @note This [[TwitterModule]] expects a [[com.twitter.finagle.thrift.ClientId]] to be bound to
 *       the object graph but does not assume how it is done. A [[com.twitter.finagle.thrift.ClientId]]
 *       can be bound by including the [[ThriftClientIdModule]] in your server configuration.
 *
 * @tparam ServicePerEndpoint A Scrooge-generated ServicePerEndpoint
 * @tparam MethodPerEndpoint  A Scrooge-generated MethodPerEndpoint
 * @see [[https://twitter.github.io/scrooge/Finagle.html#id1 MethodPerEndpoint]]
 * @see [[https://twitter.github.io/scrooge/Finagle.html#id2 ServicePerEndpoint]]
 * @see [[https://twitter.github.io/scrooge/Finagle.html#id3 ReqRepServicePerEndpoint]]
 * @see [[https://twitter.github.io/finagle/guide/Clients.html Finagle Clients]]
 * @see [[https://twitter.github.io/finagle/guide/FAQ.html?highlight=thriftmux#what-is-thriftmux What is ThriftMux?]]
 */
abstract class ThriftMethodBuilderClientModule[ServicePerEndpoint <: Filterable[ServicePerEndpoint], MethodPerEndpoint](
  implicit servicePerEndpointBuilder: ServicePerEndpointBuilder[ServicePerEndpoint],
  methodPerEndpointBuilder: MethodPerEndpointBuilder[ServicePerEndpoint, MethodPerEndpoint]
) extends TwitterModule
  with ThriftClientModuleTrait {

  protected def sessionAcquisitionTimeout: Duration = Duration.Top

  protected def requestTimeout: Duration = Duration.Top

  protected def retryBudget: Budget = Budget.default

  protected def monitor: Monitor = NullMonitor

  protected def configureThriftMuxClient(
    client: ThriftMux.Client
  ): ThriftMux.Client = client

  /**
   * This method allows for extended configuration of the base MethodBuilder (e.g., the MethodBuilder
   * used as a starting point for all method configurations) not exposed by this module or for
   * overriding provided defaults, e.g.,
   *
   * {{{
   *   override def configureMethodBuilder(methodBuilder: thriftmux.MethodBuilder): thriftmux.MethodBuilder = {
   *     methodBuilder
   *       .withTimeoutTotal(5.seconds)
   *   }
   * }}}
   *
   * Note: any configuration here will be applied to all methods unless explicitly overridden. However,
   * also note that filters are cumulative. Thus filters added here will be present in any final configuration.
   *
   * @param methodBuilder the [[thriftmux.MethodBuilder]] to configure.
   * @return a configured MethodBuilder which will be used as the starting point for any per-method
   *         configuration.
   */
  protected def configureMethodBuilder(
    methodBuilder: thriftmux.MethodBuilder
  ): thriftmux.MethodBuilder = methodBuilder

  /**
   * Configure the ServicePerEndpoint. This is done by using the given [[ThriftMethodBuilderFactory]]
   * to configure a [[com.twitter.inject.thrift.ThriftMethodBuilder]] for a given ThriftMethod. E.g.,
   *
   * {{{
   *      servicePerEndpoint
   *        .withFetchBlob(
   *          builder.method(FetchBlob)
   *          ...
   * }}}
   *
   * Subclasses of this module MAY provide an implementation of `configureServicePerEndpoint` which
   * specifies configuration of a `ServicePerEndpoint` interface per-method of the interface.
   *
   * @param builder a [[ThriftMethodBuilderFactory]] for creating a [[com.twitter.inject.thrift.ThriftMethodBuilder]].
   * @param servicePerEndpoint the [[ServicePerEndpoint]] to configure.
   * @return a per-method filtered [[ServicePerEndpoint]]
   * @see [[com.twitter.inject.thrift.ThriftMethodBuilder]]
   */
  protected def configureServicePerEndpoint(
    builder: ThriftMethodBuilderFactory[ServicePerEndpoint],
    servicePerEndpoint: ServicePerEndpoint
  ): ServicePerEndpoint = servicePerEndpoint

  @Provides
  @Singleton
  final def providesMethodPerEndpoint(
    servicePerEndpoint: ServicePerEndpoint
  ): MethodPerEndpoint = {
    ThriftMux.Client
      .methodPerEndpoint[ServicePerEndpoint, MethodPerEndpoint](servicePerEndpoint)
  }

  @Provides
  @Singleton
  final def providesServicePerEndpoint(
    injector: Injector,
    clientId: ClientId,
    statsReceiver: StatsReceiver
  ): ServicePerEndpoint = {
    createThriftMuxClient(clientId, statsReceiver)

    val methodBuilder =
      configureMethodBuilder(thriftMuxClient.methodBuilder(dest))

    val configuredServicePerEndpoint = configureServicePerEndpoint(
      builder = new ThriftMethodBuilderFactory[ServicePerEndpoint](
        injector,
        methodBuilder
      ),
      servicePerEndpoint = methodBuilder.servicePerEndpoint[ServicePerEndpoint]
    )

    closeOnExit {
      val closable = asClosable(configuredServicePerEndpoint)
      Await.result(
        closable.close(defaultClosableGracePeriod), defaultClosableAwaitPeriod)
    }
    configuredServicePerEndpoint
  }

  /* Private */

  // We want each module to be able to configure a ThriftMux.client independently
  // and it is needed by the instances to be exposed to the object graph, however we do
  // not want the client to be exposed in the object graph as including multiple modules
  // in a server would attempt to bind the same type multiple times which would error.
  // Thus we use mutation to create and configure a ThriftMux.Client.
  private[this] var thriftMuxClient: ThriftMux.Client = _
  private[this] def createThriftMuxClient(
    clientId: ClientId,
    statsReceiver: StatsReceiver
  ): Unit = {
    val clientStatsReceiver = statsReceiver.scope("clnt")

    thriftMuxClient = configureThriftMuxClient(
      ThriftMux.client.withSession
        .acquisitionTimeout(sessionAcquisitionTimeout)
        .withRequestTimeout(requestTimeout)
        .withStatsReceiver(clientStatsReceiver)
        .withClientId(clientId)
        .withMonitor(monitor)
        .withLabel(label)
        .withRetryBudget(retryBudget.retryBudget)
        .withRetryBackoff(retryBudget.requeueBackoffs)
    )
  }
}
