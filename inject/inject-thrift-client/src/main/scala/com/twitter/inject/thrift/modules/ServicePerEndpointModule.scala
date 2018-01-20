package com.twitter.inject.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.thrift.service.{Filterable, MethodPerEndpointBuilder, ServicePerEndpointBuilder}
import com.twitter.finagle.{ThriftMux, thriftmux}
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Duration, Monitor, NullMonitor, Return, Throw}
import javax.inject.Singleton

/**
 * Provides bindings for a Scrooge-generated `ServicePerEndpoint` and `MethodPerEndpoint`. The
 * `MethodPerEndpoint` is constructed via the [[MethodPerEndpointBuilder]] and is thus implemented
 * as a thin wrapper over the `ServicePerEndpoint`.
 *
 * This [[TwitterModule]] allows users to configure a Scrooge-generated `ServicePerEndpoint` which
 * can then be used directly or can be wrapped by a `MethodPerEndpoint`.
 *
 * Note when applying filters that filter order matters. The order in which filters are applied
 * is the order in which requests will flow through to the service and the opposite of the order
 * in which responses return. See the [[ThriftMethodBuilderFactory]] for more information.
 *
 * @tparam ServicePerEndpoint A Scrooge-generated ServicePerEndpoint
 * @tparam MethodPerEndpoint  A Scrooge-generated MethodPerEndpoint
 */
// TODO: remove deprecated ThriftClientModule and rename this to ThriftClientModule
abstract class ServicePerEndpointModule[ServicePerEndpoint <: Filterable[ServicePerEndpoint], MethodPerEndpoint](
  implicit servicePerEndpointBuilder: ServicePerEndpointBuilder[ServicePerEndpoint],
  methodPerEndpointBuilder: MethodPerEndpointBuilder[ServicePerEndpoint, MethodPerEndpoint]
) extends TwitterModule {

  /**
   * ThriftMux client label.
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#observability Clients Observability]]
   */
  val label: String

  /**
   * Destination of ThriftMux client.
   * @see [[https://twitter.github.io/finagle/guide/Names.html Names and Naming in Finagle]]
   */
  val dest: String

  /**
   * Configures the session acquisition `timeout` of this client (default: unbounded).
   *
   * @return a [[Duration]] which represents the acquisition timeout
   * @see [[com.twitter.finagle.param.ClientSessionParams.acquisitionTimeout]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#timeouts-expiration]]
   */
  protected def sessionAcquisitionTimeout: Duration = Duration.Top

  /**
   * Configures a "global" request `timeout` on the ThriftMux client (default: unbounded).
   * Note there is the option to configure request timeouts per-method via configuring the
   * [[com.twitter.inject.thrift.ThriftMethodBuilder]] for a given method. When applying
   * configuration per-method it is recommended to configure timeouts per-method as well.
   *
   * However, this exists in the case you want *all* requests to *every* method to have
   * the same total timeout.
   *
   * @return a [[Duration]] which represents the total request timeout
   * @see [[com.twitter.finagle.param.CommonParams.withRequestTimeout]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#timeouts-expiration]]
   * @see [[com.twitter.inject.thrift.ThriftMethodBuilder.withTimeoutPerRequest]]
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withTimeoutPerRequest]]
   * @see [[com.twitter.inject.thrift.ThriftMethodBuilder.withTimeoutTotal]]
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withTimeoutTotal]]
   */
  protected def requestTimeout: Duration = Duration.Top

  /**
   * Default [[com.twitter.finagle.service.RetryBudget]]. It is highly recommended that budgets
   * be shared between all filters that retry or re-queue requests to prevent retry storms.
   *
   * @return a default [[com.twitter.finagle.service.RetryBudget]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#retries]]
   */
  protected def retryBudget: Budget = Budget.default

  /**
   * Function to add a user-defined Monitor. A [[com.twitter.finagle.util.DefaultMonitor]] will be
   * installed implicitly which handles all exceptions caught in the stack. Exceptions that are not
   * handled by a user-defined monitor are propagated to the [[com.twitter.finagle.util.DefaultMonitor]].
   *
   * NullMonitor has no influence on DefaultMonitor behavior here.
   */
  protected def monitor: Monitor = NullMonitor

  @Provides
  @Singleton
  final def providesMethodPerEndpoint(
    servicePerEndpoint: ServicePerEndpoint
  ): MethodPerEndpoint = {
    assert(thriftMuxClient != null, "Unexpected order of initialization.")
    thriftMuxClient
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
      configureMethodBuilder(
        thriftMuxClient.methodBuilder(dest))

    configureServicePerEndpoint(
      builder = new ThriftMethodBuilderFactory[ServicePerEndpoint](
        injector,
        methodBuilder
      ),
      servicePerEndpoint = thriftMuxClient.servicePerEndpoint(dest, label)
    )
  }

  /* Protected */

  protected val PossiblyRetryableExceptions: ResponseClassifier =
    ResponseClassifier.named("PossiblyRetryableExceptions") {
      case ReqRep(_, Throw(t)) if PossiblyRetryable.possiblyRetryable(t) =>
        ResponseClass.RetryableFailure
      case ReqRep(_, Throw(_)) =>
        ResponseClass.NonRetryableFailure
      case ReqRep(_, Return(_)) =>
        ResponseClass.Success
    }

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
   * This method allows for further configuration of the ThriftMux client for parameters not exposed by
   * this module or for overriding defaults provided herein, e.g.,
   *
   * {{{
   *   override def configureThriftMuxClient(client: ThriftMux.Client): ThriftMux.Client = {
   *     client
   *       .withProtocolFactory(myCustomProtocolFactory))
   *       .withStatsReceiver(someOtherScopedStatsReceiver)
   *       .withMonitor(myAwesomeMonitor)
   *       .withTracer(notTheDefaultTracer)
   *       .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)
   *   }
   *}}}
   *
   * @param client the [[com.twitter.finagle.ThriftMux.Client]] to configure.
   * @return a configured ThriftMux.Client.
   */
  protected def configureThriftMuxClient(
    client: ThriftMux.Client
  ): ThriftMux.Client = client

  /**
   * Configure the ServicePerEndpoint. This is done by using the given [[ThriftMethodBuilderFactory]]
   * to configure a [[com.twitter.inject.thrift.ThriftMethodBuilder]] for a given ThriftMethod. E.g.,
   *
   *      servicePerEndpoint
   *        .withFetchBlob(
   *          builder.method(FetchBlob)
   *          ...
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

  /* Private */

  // We want each module to be able to configure a ThriftMux.client independently,
  // however we do not want the client to be exposed in the object graph as including
  // multiple modules in a server would attempt to bind the same type multiple times
  // which would fail. Thus we use mutation to create and configure a ThriftMux.Client.
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
