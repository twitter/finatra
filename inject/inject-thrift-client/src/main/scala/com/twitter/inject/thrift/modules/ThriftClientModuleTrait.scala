package com.twitter.inject.thrift.modules

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.service.Retries.Budget
import com.twitter.util.{Duration, Monitor}

private[inject] trait ThriftClientModuleTrait {

  /**
   * ThriftMux client label.
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#observability Clients Observability]]
   */
  def label: String

  /**
   * Destination of ThriftMux client.
   * @see [[https://twitter.github.io/finagle/guide/Names.html Names and Naming in Finagle]]
   */
  def dest: String

  /**
   * Configures the session acquisition `timeout` of this client (default: unbounded).
   *
   * @return a [[Duration]] which represents the acquisition timeout
   * @see [[com.twitter.finagle.param.ClientSessionParams.acquisitionTimeout]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#timeouts-expiration]]
   */
  protected def sessionAcquisitionTimeout: Duration

  /**
   * Configures a "global" request `timeout` on the ThriftMux client (default: unbounded).
   * This will set *all* requests to *every* method to have the same total timeout.
   *
   * @return a [[Duration]] which represents the total request timeout
   * @see [[com.twitter.finagle.param.CommonParams.withRequestTimeout]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#timeouts-expiration]]
   */
  protected def requestTimeout: Duration

  /**
   * Default [[com.twitter.finagle.service.RetryBudget]]. It is highly recommended that budgets
   * be shared between all filters that retry or re-queue requests to prevent retry storms.
   *
   * @return a default [[com.twitter.finagle.service.RetryBudget]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#retries]]
   */
  protected def retryBudget: Budget

  /**
   * Function to add a user-defined Monitor. A [[com.twitter.finagle.util.DefaultMonitor]] will be
   * installed implicitly which handles all exceptions caught in the stack. Exceptions that are not
   * handled by a user-defined monitor are propagated to the [[com.twitter.finagle.util.DefaultMonitor]].
   *
   * NullMonitor has no influence on DefaultMonitor behavior here.
   */
  protected def monitor: Monitor

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
   * @return a configured [[ThriftMux.Client]].
   */
  protected def configureThriftMuxClient(
    client: ThriftMux.Client
  ): ThriftMux.Client
}
