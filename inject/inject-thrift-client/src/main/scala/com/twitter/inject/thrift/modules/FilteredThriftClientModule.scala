package com.twitter.inject.thrift.modules

import com.github.nscala_time.time
import com.github.nscala_time.time.DurationBuilder
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{ClientId, MethodIfaceBuilder, ServiceIfaceBuilder}
import com.twitter.inject.annotations.Flag
import com.twitter.inject.conversions.duration._
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.inject.thrift.filters.ThriftClientFilterBuilder
import com.twitter.inject.thrift.modules.FilteredThriftClientModule.MaxDuration
import com.twitter.inject.thrift.{AndThenService, NonFiltered}
import com.twitter.inject.{Injector, RootMonitor, TwitterModule}
import com.twitter.scrooge.{ThriftResponse, ThriftService}
import com.twitter.util.{Duration => TwitterDuration, Monitor, Try}
import javax.inject.Singleton
import org.joda.time.Duration
import scala.language.implicitConversions
import scala.reflect.ClassTag

object FilteredThriftClientModule {
  val MaxDuration = Duration.millis(Long.MaxValue)
}

abstract class FilteredThriftClientModule[FutureIface <: ThriftService : ClassTag, ServiceIface: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface],
  methodBuilder: MethodIfaceBuilder[ServiceIface, FutureIface])
  extends TwitterModule
  with time.Implicits {

  override val frameworkModules = Seq(
    AndThenServiceModule,
    FilteredThriftClientFlagsModule)

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
  protected val mux: Boolean = true

  protected val useHighResTimerForRetries = false

  protected def sessionAcquisitionTimeout: Duration = MaxDuration

  protected def budget: Budget = Budget.default

  protected def monitor: Monitor = RootMonitor

  /**
   * This method allows for further configuration of the client for parameters not exposed by
   * this module or for overriding defaults provided herein, e.g.,
   *
   * override def configureThriftMuxClient(client: ThriftMux.Client): ThriftMux.Client = {
   *   client
   *     .withProtocolFactory(myCustomProtocolFactory))
   *     .withStatsReceiver(someOtherScopedStatsReceiver)
   *     .withMonitor(myAwesomeMonitor)
   *     .withTracer(notTheDefaultTracer)
   *     .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)
   * }
   *
   * @param client - the [[com.twitter.finagle.ThriftMux.Client]] to configure.
   * @return a configured ThriftMux.Client.
   */
  protected def configureThriftMuxClient(client: ThriftMux.Client): ThriftMux.Client = {
    client
  }

  /**
   * This method allows for further configuration of the client for parameters not exposed by
   * this module or for overriding defaults provided herein, e.g.,
   *
   * override def configureNonThriftMuxClient(client: Thrift.Client): Thrift.Client = {
   *   client
   *     .withProtocolFactory(myCustomProtocolFactory))
   *     .withStatsReceiver(someOtherScopedStatsReceiver)
   *     .withMonitor(myAwesomeMonitor)
   *     .withTracer(notTheDefaultTracer)
   *     .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)
   * }
   *
   * In general it is recommended that users prefer to use ThriftMux if the server-side supports
   * mux connections.
   *
   * @param client - the [[com.twitter.finagle.Thrift.Client]] to configure.
   * @return a configured Thrift.Client.
   */
  protected def configureNonThriftMuxClient(client: Thrift.Client): Thrift.Client = {
    client
  }

  /**
   * Add filters to the ServiceIface based client.
   */
  def filterServiceIface(
    serviceIface: ServiceIface,
    filters: ThriftClientFilterBuilder): ServiceIface

  @Provides
  @Singleton
  final def providesClient(
    @Flag("timeout.multiplier") timeoutMultiplier: Int,
    @Flag("retry.multiplier") retryMultiplier: Int,
    @NonFiltered serviceIface: ServiceIface,
    injector: Injector,
    statsReceiver: StatsReceiver,
    andThenService: AndThenService
  ): FutureIface = {
    val filterBuilder = new ThriftClientFilterBuilder(
      timeoutMultiplier,
      retryMultiplier,
      injector,
      statsReceiver,
      label,
      budget,
      useHighResTimerForRetries,
      andThenService)

    Thrift.client.newMethodIface(
      filterServiceIface(
        serviceIface = serviceIface,
        filters = filterBuilder))
  }

  @Provides
  @NonFiltered
  @Singleton
  final def providesUnfilteredServiceIface(
    @Flag("timeout.multiplier") timeoutMultiplier: Int,
    clientId: ClientId,
    statsReceiver: StatsReceiver): ServiceIface = {
    val acquisitionTimeout = sessionAcquisitionTimeout.toTwitterDuration * timeoutMultiplier
    val clientStatsReceiver = statsReceiver.scope("clnt")

    val thriftClient =
      if (mux) {
        configureThriftMuxClient(
          ThriftMux.client
            .withSession.acquisitionTimeout(acquisitionTimeout)
            .withStatsReceiver(clientStatsReceiver)
            .withClientId(clientId)
            .withMonitor(monitor)
            .withRetryBudget(budget.retryBudget)
            .withRetryBackoff(budget.requeueBackoffs))
      }
      else {
        configureNonThriftMuxClient(
          Thrift.client
            .withSession.acquisitionTimeout(acquisitionTimeout)
            .withStatsReceiver(clientStatsReceiver)
            .withClientId(clientId)
            .withMonitor(monitor)
            .withRetryBudget(budget.retryBudget)
            .withRetryBackoff(budget.requeueBackoffs))
      }

    thriftClient
      .newServiceIface[ServiceIface](dest, label)
  }

  /* Common Retry Functions */

  protected val PossiblyRetryableExceptions: PartialFunction[Try[ThriftResponse[_]], Boolean] =
    PossiblyRetryable.PossiblyRetryableExceptions

  /* Common Implicits */

  implicit def toTwitterDuration(duration: DurationBuilder): TwitterDuration = {
    TwitterDuration.fromMilliseconds(duration.toDuration.getMillis)
  }
}