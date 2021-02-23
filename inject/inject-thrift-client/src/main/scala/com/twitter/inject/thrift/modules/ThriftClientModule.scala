package com.twitter.inject.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.Await
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

  override protected final def initialClientConfiguration(
    injector: Injector,
    client: ThriftMux.Client,
    statsReceiver: StatsReceiver
  ): ThriftMux.Client = super.initialClientConfiguration(injector, client, statsReceiver)

  override protected final def scopeStatsReceiver(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): StatsReceiver = super.scopeStatsReceiver(injector, statsReceiver)

  @Singleton
  @Provides
  final def providesThriftClient(
    injector: Injector,
    clientId: ClientId,
    statsReceiver: StatsReceiver
  ): ThriftService = {
    val thriftmuxClient = newClient(injector, statsReceiver).build[ThriftService](dest, label)
    onExit {
      val closable = asClosableThriftService(thriftmuxClient)
      Await.result(closable.close(defaultClosableGracePeriod), defaultClosableAwaitPeriod)
    }
    thriftmuxClient
  }

}
