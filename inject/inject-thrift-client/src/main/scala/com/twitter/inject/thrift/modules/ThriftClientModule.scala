package com.twitter.inject.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.service.RetryBudget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Await, Duration, Monitor, NullMonitor}
import javax.inject.Singleton
import scala.reflect.ClassTag

/**
 * A [[TwitterModule]] allows users to configure a Finagle `ThriftMux` client and does NOT
 * provide ability to filter or configure per-method Scrooge-generated interfaces. The client
 * interface can be expressed as a `MethodPerEndpoint` or higher-kinded interface.
 *
 * Provides bindings for a Scrooge-generated `MethodPerEndpoint` or higher-kinded interface.
 *
 * See the [[ThriftMethodBuilderClientModule]] for building a `ThriftMux` client that allows for filtering
 * and configuration per-method of the Scrooge-generated interface.
 *
 * @note This [[TwitterModule]] expects a [[com.twitter.finagle.thrift.ClientId]] to be bound to
 *       the object graph but does not assume how it is done. A [[com.twitter.finagle.thrift.ClientId]]
 *       can be bound by including the [[ThriftClientIdModule]] in your server configuration.
 *
 * @tparam ThriftService A Scrooge-generated `MethodPerEndpoint` or the higher-kinded type of the
 *                       Scrooge-generated service, e.g., `MyService[Future]`.
 * @see [[com.twitter.finagle.thrift.ThriftRichClient.build(dest: String, label: String)]]
 * @see [[https://twitter.github.io/finagle/guide/Clients.html Finagle Clients]]
 * @see [[https://twitter.github.io/finagle/guide/FAQ.html?highlight=thriftmux#what-is-thriftmux What is ThriftMux?]]
 */
abstract class ThriftClientModule[ThriftService: ClassTag]
  extends TwitterModule
  with ThriftClientModuleTrait {

  protected def sessionAcquisitionTimeout: Duration = Duration.Top

  protected def requestTimeout: Duration = Duration.Top

  protected def retryBudget: RetryBudget = RetryBudget()

  protected def monitor: Monitor = NullMonitor

  protected def configureThriftMuxClient(
    injector: Injector,
    client: ThriftMux.Client
  ): ThriftMux.Client = client

  @Singleton
  @Provides
  final def providesThriftClient(
    injector: Injector,
    clientId: ClientId,
    statsReceiver: StatsReceiver
  ): ThriftService = {
    val clientStatsReceiver = statsReceiver.scope("clnt")

    val thriftService =
      configureThriftMuxClient(
        injector,
        ThriftMux.client.withSession
          .acquisitionTimeout(sessionAcquisitionTimeout)
          .withRequestTimeout(requestTimeout)
          .withStatsReceiver(clientStatsReceiver)
          .withClientId(clientId)
          .withMonitor(monitor)
          .withLabel(label)
          .withRetryBudget(retryBudget)
      ).build[ThriftService](dest, label)

    closeOnExit {
      val closable = asClosable(thriftService)
      Await.result(
        closable.close(defaultClosableGracePeriod), defaultClosableAwaitPeriod)
    }
    thriftService
  }
}
