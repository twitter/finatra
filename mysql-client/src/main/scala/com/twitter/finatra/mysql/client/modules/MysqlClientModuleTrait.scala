package com.twitter.finatra.mysql.client.modules

import com.twitter.finagle.Mysql
import com.twitter.finagle.mysql.Request
import com.twitter.finagle.mysql.Result
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.mysql.client.Config
import com.twitter.finatra.mysql.client.MysqlClient
import com.twitter.inject.Injector
import com.twitter.inject.modules.StackClientModuleTrait
import com.twitter.util.Await

/**
 * Extending this trait allows for configuring an
 * [[com.twitter.finagle.Mysql.Client]] and/or a [[com.twitter.finatra.mysql.client.MysqlClient]].
 *
 * @example
 *          {{{
 *            class ExampleMysqlClientModule
 *              extends MysqlClientModuleTrait {
 *
 *              override def dest: String = "/s/my/service"
 *              override def label: String = "myservice"
 *
 *              override protected def sessionAcquisitionTimeout: Duration = 1.seconds
 *              override protected def requestTimeout: Duration = 5.seconds
 *              override protected def retryBudget: RetryBudget = RetryBudget(15.seconds, 5, .1)
 *
 *              // if you want to customize the client configuration
 *              // you can:
 *              //
 *              // override def configureClient(
 *              //  injector: Injector,
 *              //  client: Mysql.Client
 *              // ): Mysql.Client =
 *              //   client.
 *              //     withTracer(NullTracer)
 *              //     withStatsReceiver(NullStatsReceiver)
 *              //
 *              // Due to the finagle-mysql API's reliance upon mixin traits on top of the standard
 *              // Mysql.Client, providing and binding the more friendly
 *              // `com.twitter.finatra.mysql.client.MysqlClient` wrapper should be preferred
 *              // over other methods (such as supplying a Service or Mysql.Client)
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideExampleMysqlClient(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver,
 *              //   config: Config
 *              // ): MysqlClient = newMysqlClient(injector, statsReceiver, config)
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideExampleMysqlClient(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver
 *              // ): MysqlClient = newMysqlClient(
 *              //      injector,
 *              //      statsReceiver,
 *              //      Config("database", Credentials("user", "pass")))
 *              //
 *              // depending on your client type, you may want to provide a global instance,
 *              // otherwise you might want to specify how your consumers can provide a binding
 *              // for an instance to the client
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideFinagleClient(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver
 *              // ): Mysql.Client = newClient(injector, statsReceiver)
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
 *              //
 *            }
 *          }}}
 *
 * @note This trait does not define any `@Provide` annotations or bindings.
 */
trait MysqlClientModuleTrait extends StackClientModuleTrait[Request, Result, Mysql.Client] {

  override protected def baseClient: Mysql.Client = Mysql.client

  /** Do not call directly. Prefer [[newMysqlClient]]. */
  override protected final def newClient(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Mysql.Client = super.newClient(injector, statsReceiver)

  // note: can also be bound to `Client with Transactions`
  protected final def newMysqlClient(
    injector: Injector,
    statsReceiver: StatsReceiver,
    config: Config
  ): MysqlClient = {
    val baseClient = newClient(injector, statsReceiver, config).newRichClient(dest, label)
    val clnt = MysqlClient(baseClient)
    onExit {
      Await.result(clnt.close(defaultClosableGracePeriod), defaultClosableAwaitPeriod)
    }
    clnt
  }

  private[this] def newClient(
    injector: Injector,
    statsReceiver: StatsReceiver,
    config: Config
  ): Mysql.Client =
    newClient(injector, statsReceiver)
      .withDatabase(config.databaseName)
      .withCredentials(config.credentials.user, config.credentials.password)

}
