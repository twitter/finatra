package com.twitter.inject.thrift.modules

import com.github.nscala_time.time
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.loadbalancer.{LoadBalancerFactory, DefaultBalancerFactory}
import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{ClientId, MethodIfaceBuilder, ServiceIfaceBuilder}
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.inject.conversions.duration._
import com.twitter.inject.thrift.NonFiltered
import com.twitter.inject.thrift.filters.FilterBuilder
import com.twitter.inject.thrift.modules.FilteredThriftClientModule.MaxDuration
import com.twitter.scrooge.{ThriftResponse, ThriftService}
import com.twitter.util.{Return, Throw, Try}
import javax.inject.Singleton
import org.joda.time.Duration
import scala.reflect.ClassTag

object FilteredThriftClientModule {
  val MaxDuration = Duration.millis(Long.MaxValue)
}

abstract class FilteredThriftClientModule[FutureIface <: ThriftService : ClassTag, ServiceIface: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface],
  methodBuilder: MethodIfaceBuilder[ServiceIface, FutureIface])
  extends TwitterModule
  with time.Implicits {

  /**
   * Name of client for use in metrics
   */
  val label: String

  /**
   * Destination of client (usually a wily path)
   */
  val dest: String

  /**
    * Enable thrift mux for this connection.
    *
    * Note: Both server and client must have mux enabled otherwise
    * a nondescript ChannelClosedException will be seen.
    */
  protected val mux: Boolean = true

  protected def sessionAcquisitionTimeout: Duration = MaxDuration

  protected def loadBalancer: LoadBalancerFactory = DefaultBalancerFactory

  protected def configureThriftMuxClient(thriftMuxClient: ThriftMux.Client): ThriftMux.Client = {
    thriftMuxClient
  }

  protected def configureNonThriftMuxClient(thriftClient: Thrift.Client): Thrift.Client = {
    thriftClient
  }

  /**
   * Add filters to the ServiceIface based client
   */
  def filterServiceIface(
    serviceIface: ServiceIface,
    filter: FilterBuilder): ServiceIface

  @Provides
  @Singleton
  final def providesClient(
    @NonFiltered serviceIface: ServiceIface,
    injector: Injector,
    statsReceiver: StatsReceiver): FutureIface = {

    val filterBuilder = new FilterBuilder(
      injector,
      statsReceiver,
      label)

    Thrift.newMethodIface(
      filterServiceIface(
        serviceIface = serviceIface,
        filter = filterBuilder))
  }

  @Provides
  @NonFiltered
  @Singleton
  final def providesUnfilteredServiceIface(
    clientId: ClientId,
    statsReceiver: StatsReceiver): ServiceIface = {
    val acquisitionTimeout = sessionAcquisitionTimeout.toTwitterDuration
    val clientStatsReceiver = statsReceiver.scope("clnt")

    if (mux) {
      val thriftMuxClient = ThriftMux.client
        .withSession.acquisitionTimeout(acquisitionTimeout)
        .withStatsReceiver(clientStatsReceiver)
        .withLoadBalancer(loadBalancer)
        .withClientId(clientId)

      configureThriftMuxClient(thriftMuxClient)
        .newServiceIface[ServiceIface](dest = dest, label = label)
    }
    else {
      val thriftClient = Thrift.client
        .withSession.acquisitionTimeout(acquisitionTimeout)
        .withStatsReceiver(clientStatsReceiver)
        .withLoadBalancer(loadBalancer)
        .withClientId(clientId)

      configureNonThriftMuxClient(thriftClient)
        .newServiceIface[ServiceIface](dest = dest, label = label)
    }
  }

  /* Common Retry Functions */

  lazy val PossiblyRetryableExceptions: PartialFunction[Try[ThriftResponse[_]], Boolean] = {
    case Throw(t) => possiblyRetryable(t)
    case Return(response) => response.firstException exists possiblyRetryable
  }

  def possiblyRetryable(t: Throwable): Boolean = {
    !isCancellation(t)
  }

  def isCancellation(t: Throwable): Boolean = t match {
    case _: CancelledRequestException => true
    case _: CancelledConnectionException => true
    case _: ClientDiscardedRequestException => true
    case f: Failure if f.isFlagged(Failure.Interrupted) => true
    case f: Failure if f.cause.isDefined => isCancellation(f.cause.get)
    case _ => false
  }
}