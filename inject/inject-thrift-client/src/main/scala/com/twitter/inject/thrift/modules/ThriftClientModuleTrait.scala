package com.twitter.inject.thrift.modules

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.inject.Injector
import com.twitter.inject.modules.StackClientModuleTrait
import com.twitter.scrooge.AsClosableMethodName
import com.twitter.util.Closable
import com.twitter.util.logging.Logging

private[twitter] trait ThriftClientModuleTrait
    extends StackClientModuleTrait[ThriftClientRequest, Array[Byte], ThriftMux.Client]
    with Logging {

  override protected final def baseClient: ThriftMux.Client = ThriftMux.client

  /**
   * Override to supply a custom [[ClientId]], e.g.,
   *   override protected def clientId(injector: Injector): ClientId = ClientId("myclient")
   * }}}
   *
   * @note The default requires a [[ClientId]] be bound to the [[injector]],
   *       for example via a [[ThriftClientIdModule]].
   */
  protected def clientId(injector: Injector): ClientId = injector.instance[ClientId]

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
   * @param injector the [[com.twitter.inject.Injector]] which can be used to help configure the
   *                 given [[com.twitter.finagle.ThriftMux.Client]].
   * @param client the [[com.twitter.finagle.ThriftMux.Client]] to configure.
   * @return a configured [[ThriftMux.Client]].
   */
  protected def configureThriftMuxClient(
    injector: Injector,
    client: ThriftMux.Client
  ): ThriftMux.Client = client

  // while this trait is the preferred configuration method for the StackClientModuleTrait,
  // this class pre-dates the StackClientModuleTrait and the user facing API has been to
  // use `configureThriftMuxClient`, which we will retain by making this final.
  override protected final def configureClient(
    injector: Injector,
    client: ThriftMux.Client
  ): ThriftMux.Client = configureThriftMuxClient(injector, client)

  // Java friendly: https://issues.scala-lang.org/browse/SI-8905
  override protected final def newClient(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): ThriftMux.Client = super.newClient(injector, statsReceiver)

  override protected def initialClientConfiguration(
    injector: Injector,
    client: ThriftMux.Client,
    statsReceiver: StatsReceiver
  ): ThriftMux.Client =
    super
      .initialClientConfiguration(injector, client, statsReceiver)
      .withClientId(clientId(injector))

  /* Private */

  override protected def asClosable(client: ThriftMux.Client): Closable = asClosableThriftService(
    client)

  private[modules] def asClosableThriftService(thriftService: Any): Closable = {
    thriftService match {
      case closable: Closable => closable
      case _ =>
        val asClosableMethodOpt =
          thriftService.getClass.getMethods
            .find(_.getName == AsClosableMethodName)
        asClosableMethodOpt match {
          case Some(method) =>
            try {
              method.invoke(thriftService).asInstanceOf[Closable]
            } catch {
              case _: java.lang.ClassCastException =>
                warn(
                  s"Unable to cast result of ${AsClosableMethodName} invocation to a " +
                    s"${Closable.getClass.getName.dropRight(1)} type."
                )
                Closable.nop
            }
          case _ =>
            warn(
              s"${AsClosableMethodName} not found for instance: ${thriftService.getClass.getName}"
            )
            Closable.nop
        }
    }
  }
}
