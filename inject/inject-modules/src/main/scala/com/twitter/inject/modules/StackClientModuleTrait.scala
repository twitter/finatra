package com.twitter.inject.modules

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Service, Stack}
import com.twitter.finagle.client.StackBasedClient
import com.twitter.finagle.param.{ClientParams, CommonParams, WithClientSession}
import com.twitter.finagle.service.RetryBudget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Await, Closable, Duration, Monitor, NullMonitor}

/**
 * A module for configuring a Finagle [[StackBasedClient]]. Binding is explicitly not handled by this
 * trait and implementors are responsible for managing their own binding annotations.
 *
 * @example
 *          {{{
 *            abstract class MyClientModule
 *              extends StackClientModuleTrait[Request, Response, MyClient] {
 *
 *              override protected final def baseClient: MyClient = MyClient.client
 *              override protected def sessionAcquisitionTimeout: Duration = 1.seconds
 *              override protected def requestTimeout: Duration = 5.seconds
 *              override protected def retryBudget: RetryBudget = RetryBudget(15.seconds, 5, .1)
 *
 *              // if you want to customize the client configuration
 *              // you can:
 *              //
 *              // override def configureClient(injector: Injector, client: MyClient): MyClient =
 *              //   client.
 *              //     withTracer(NullTracer)
 *              //     withStatsReceiver(NullStatsReceiver)
 *              //
 *              // depending on your client type, you may want to provide a global instance,
 *              // otherwise you might want to specify how your consumers can provide a binding
 *              // for an instance to the client
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideMyClient(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver
 *              //  ): MyClient =
 *              //    newClient(injector, statsReceiver)
 *              //
 *              // Or create a service directly
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideMyService(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver
 *              // ): Service[Request, Response] =
 *              //     myCoolFilter.andThen(newService(injector, statsReceiver))
 *            }
 *          }}}
 * @note Extending this module for HTTP and ThriftMux clients should not be necessary, as there
 *       are fully supported modules for creating those clients.
 *
 * @note The ordering of client configuration may be important. The underlying clients will be
 *       configured and created in the following order:
 *         baseClient -> initialClientConfiguration -> configureClient -> frameworkConfigureClient
 */
trait StackClientModuleTrait[
  Req,
  Rep,
  ClientType <: StackBasedClient[Req, Rep] with Stack.Parameterized[ClientType] with CommonParams[
    ClientType
  ] with ClientParams[ClientType] with WithClientSession[ClientType]]
    extends TwitterModule {

  /**
   * Finagle client label.
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#observability Clients Observability]]
   */
  def label: String

  /**
   * Destination of Finagle client.
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
  protected def sessionAcquisitionTimeout: Duration = Duration.Top

  /**
   * Configures a "global" request `timeout` on the Finagle client (default: unbounded).
   * This will set *all* requests to *every* method to have the same total timeout.
   *
   * @return a [[Duration]] which represents the total request timeout
   *
   * @see [[com.twitter.finagle.param.CommonParams.withRequestTimeout]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#timeouts-expiration]]
   */
  protected def requestTimeout: Duration = Duration.Top

  /**
   * Default [[com.twitter.finagle.service.RetryBudget]]. It is highly recommended that budgets
   * be shared between all filters that retry or re-queue requests to prevent retry storms.
   *
   * @return a default [[com.twitter.finagle.service.RetryBudget]]
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#retries]]
   */
  protected def retryBudget: RetryBudget = RetryBudget()

  /**
   * Function to add a user-defined Monitor. A [[com.twitter.finagle.util.DefaultMonitor]] will be
   * installed implicitly which handles all exceptions caught in the stack. Exceptions that are not
   * handled by a user-defined monitor are propagated to the [[com.twitter.finagle.util.DefaultMonitor]].
   *
   * NullMonitor has no influence on DefaultMonitor behavior here.
   */
  protected def monitor: Monitor = NullMonitor

  /**
   * Create a base Finagle Stack Client of type [[ClientType]]. This method should not try to do
   * any configuration on the created client.
   *
   * @example {{{ override def createBaseClient(): Http.Client = Http.client }}}
   * @example {{{ override def createBaseClient(): ThriftMux.Client = ThriftMux.client }}}
   * @example {{{ override def createBaseClient(): Memcached.Client =  Memcached.client }}}
   *
   * @return The base [[ClientType]] client, without any custom configuration.
   */
  protected def baseClient: ClientType

  /**
   * Initial configuration of the underlying client. This is exposed to allow for customization
   * of the base client configuration. All user facing extensions should be done via [[configureClient]].
   *
   * @example {{{
   *         override protected final def initialClientConfiguration(
   *           injector: Injector,
   *           client: ClientType,
   *           statsReceiver: StatsReceiver
   *         ): ThriftMux.Client =
   *           super.initialClientConfiguration(injector, client, statsReceiver)
   *             .withClientId(injector.instance[ClientId]
   *         }}}
   *
   * @return The base configured [[ClientType]] client, without any custom end-user configuration.
   */
  protected def initialClientConfiguration(
    injector: Injector,
    client: ClientType,
    statsReceiver: StatsReceiver
  ): ClientType = {
    client.withSession
      .acquisitionTimeout(sessionAcquisitionTimeout)
      .withRequestTimeout(requestTimeout)
      .withStatsReceiver(statsReceiver)
      .withMonitor(monitor)
      .withLabel(label)
      .withRetryBudget(retryBudget)
  }

  /**
   * This method allows for further configuration of the [[ClientType]] client for parameters not exposed by
   * this module or for overriding defaults provided herein, e.g.,
   *
   * {{{
   *   override protected def configureClient(client: Example.Client): Example.Client = {
   *     client
   *       .withStatsReceiver(someOtherScopedStatsReceiver)
   *       .withMonitor(myAwesomeMonitor)
   *       .withTracer(notTheDefaultTracer)
   *   }
   *}}}
   *
   * @param injector the [[com.twitter.inject.Injector]] which can be used to help configure the
   *                 given [[ClientType]] client.
   * @param client the [[ClientType]] client to configure.
   * @return a configured [[ClientType]] client.
   */
  protected def configureClient(injector: Injector, client: ClientType): ClientType = client

  /**
   * Provide a customized base scope for the [[StatsReceiver]] exposed by this module, e.g.,
   * *
   * * {{{
   *    override protected def scopeStatsReceiver(injector: Injector, statsReceiver: StatsReceiver): StatsReceiver =
   *     statsReceiver.scope("clnt", "custom_scope")
   *   }}}
   *
   * @note The default scope is "clnt".
   * @note Changing the default scope can have negative impacts on observability of metrics. Use
   *       caution when changing this value.
   */
  protected def scopeStatsReceiver(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): StatsReceiver =
    statsReceiver.scope("clnt")

  /**
   * This method should be overridden by implementors IF the [[ClientType]] does not
   * extend [[Closable]]. This method should wrap an underlying client as a Closable
   * to ensure that resources are dealt with cleanly upon shutdown.
   *
   * @example
   *          {{{
   *             override protected def asClosable(client: ClientType): Closable =
   *               clientType.asClosable
   *          }}}
   *
   * @example
   *          {{{
   *            override protected def asClosable(client: ClientType): Closable =
   *              Closable.make { deadline =>
   *                // Use a FuturePool to ensure the task is completed asynchronously
   *                // and allow for enforcing the deadline Time.
   *                FuturePool
   *                  .unboundedPool {
   *                    clientType.closeSession() // clean-up resources
   *                  }.by(deadline)(DefaultTimer)
   *              }
   *          }}}
   *
   * @param client The client that does not extend Closable
   *
   * @return The [[Closable]] whose logic cleans up `client`'s resources
   */
  protected def asClosable(client: ClientType): Closable = {
    warn(
      s"${client.getClass.getName} is not a com.twitter.util.Closable. " +
        "Implementers of the StackClientModuleTrait should override the `asClosable(client)` " +
        "method in order to ensure that resources are dealt with cleanly upon shutdown."
    )
    Closable.nop
  }

  /**
   * This method will generate a fully configured [[ClientType]]
   *
   * @param injector the [[com.twitter.inject.Injector]] which can be used to help configure the
   *                 given [[ClientType]] client.
   * @param statsReceiver The [[StatsReceiver]] to use with the generated [[ClientType]]
   *
   * @return A configured [[ClientType]]
   *
   * @note The ability to override this method is exposed for Java compatibility purposes, where the
   *       type information in this trait can be erased from this trait to a more generic
   *       [[ClientType]], which can cause Java compilation failures. See
   *       https://issues.scala-lang.org/browse/SI-8905.
   *       When overriding this method, it should treat this method as final, outside of providing
   *       a more specific return type. For example:
   *
   *       {{{
   *         override protected final def newClient(
   *           injector: Injector,
   *           statsReceiver: StatsReceiver
   *         ): Http.Client = super.newClient(injector, statsReceiver)
   *       }}}
   */
  protected def newClient(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): ClientType = {
    val clientStatsReceiver = scopeStatsReceiver(injector, statsReceiver)

    // the `baseClient` will be configured with the properties exposed by this trait,
    // followed by any custom configuration provided by overriding `configureClient`,
    // and finally applying the `frameworkConfigureClient` configuration
    frameworkConfigureClient(
      injector,
      configureClient(
        injector,
        initialClientConfiguration(injector, baseClient, clientStatsReceiver)
      )
    )
  }

  /**
   * This method will generate a `Service[Req, Rep]` from the configured [[ClientType]]
   * generated by calling [[newClient()]].
   *
   * @param injector the [[com.twitter.inject.Injector]] which can be used to help configure the
   *                 given [[ClientType]] client.
   * @param statsReceiver The [[StatsReceiver]] to use with the generated `Service[Req, Rep]`.
   *
   * @return A `Service[Req, Rep]` that overlays the [[ClientType]]
   */
  protected final def newService(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Service[Req, Rep] = {
    val service = newClient(injector, statsReceiver).newService(dest, label)
    onExit {
      Await.result(service.close(defaultClosableGracePeriod), defaultClosableAwaitPeriod)
    }
    service
  }

  /* Configuration of the client reserved by the framework */
  protected[twitter] def frameworkConfigureClient(
    injector: Injector,
    client: ClientType
  ): ClientType = client
}
