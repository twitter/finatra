package com.twitter.inject.thrift.modules

import com.twitter.conversions.time._
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.service.RetryBudget
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.ThriftService
import com.twitter.util.{Closable, Duration, Monitor}

private[inject] trait ThriftClientModuleTrait extends Logging {

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
   * Default amount of time to wait for any [[Closable]] being registered in a `closeOnExit` block.
   * Note that this timeout is advisory, as it attempts to give the close function some leeway, for
   * example to drain clients or finish up other tasks.
   *
   * @return a [[com.twitter.util.Duration]]
   * @see [[com.twitter.util.Closable.close(after: Duration)]]
   */
  protected def defaultClosableGracePeriod: Duration = 1.second

  /**
   * Default amount of time to block in [[com.twitter.util.Awaitable.result(timeout: Duration)]] on
   * a [[Closable]] to close that is registered in a `closeOnExit` block.
   *
   * @return a [[com.twitter.util.Duration]]
   * @see [[com.twitter.util.Awaitable.result(timeout: Duration)]]
   */
  protected def defaultClosableAwaitPeriod: Duration = 2.seconds

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
  protected def retryBudget: RetryBudget

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
   *       .withClientId(injector.instance[ClientId])
   *       .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)
   *   }
   *}}}
   *
   * @param injector the [[com.twitter.inject.Injector]] which can be used to help configure the
   *                 given [[com.twitter.finagle.ThriftMux.Client]].
   * @param client the [[com.twitter.finagle.ThriftMux.Client]] to configure.
   * @return a configured [[ThriftMux.Client]].
   */
  protected def configureThriftMuxClient(
    injector: Injector,
    client: ThriftMux.Client
  ): ThriftMux.Client

  /* Private */

  private[modules] def asClosable(thriftService: Any): Closable = {
    thriftService match {
      case closable: Closable => closable
      case _ =>
        val asClosableMethodOpt =
          thriftService
            .getClass
            .getDeclaredMethods
            .find(_.getName == ThriftService.AsClosableMethodName)
        asClosableMethodOpt match {
          case Some(method) =>
            try {
              method.invoke(thriftService).asInstanceOf[Closable]
            } catch {
              case _: java.lang.ClassCastException =>
                warn(
                  s"Unable to cast result of ${ThriftService.AsClosableMethodName} invocation to a " +
                    s"${Closable.getClass.getName.dropRight(1)} type."
                )
                Closable.nop
            }
          case _ =>
            warn(
              s"${ThriftService.AsClosableMethodName} not found for instance: ${thriftService.getClass.getName}"
            )
            Closable.nop
        }
    }
  }
}
