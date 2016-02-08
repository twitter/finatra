package com.twitter.inject.thrift

import com.github.nscala_time.time
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.factory.TimeoutFactory
import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.param.Stats
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{MethodIfaceBuilder, ClientId, ServiceIfaceBuilder}
import com.twitter.inject.TwitterModule
import com.twitter.inject.conversions.DurationConversions
import com.twitter.inject.thrift.FilteredThriftClientModule.MaxDuration
import com.twitter.scrooge.{ThriftResponse, ThriftService}
import com.twitter.util.{Return, Throw, Try}
import javax.inject.Singleton
import org.joda.time.Duration
import scala.reflect.ClassTag

object FilteredThriftClientModule {
  val MaxDuration = Duration.millis(Long.MaxValue)
}

abstract class FilteredThriftClientModule[FutureIface <: ThriftService : ClassTag, ServiceIface: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface], methodBuilder: MethodIfaceBuilder[ServiceIface, FutureIface])
  extends TwitterModule
  with DurationConversions
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
   * a non-descript ChannelClosedException will be seen.
   */
  val mux: Boolean = true

  def requestTimeout: Duration = MaxDuration

  def connectTimeout: Duration = MaxDuration

  /**
   * Add filters to the ServiceIface based client
   */
  def filterServiceIface(
    serviceIface: ServiceIface,
    filters: FilterBuilder): ServiceIface

  @Provides
  @Singleton
  def providesClient(
    @NonFiltered serviceIface: ServiceIface,
    filter: FilterBuilder,
    statsReceiver: StatsReceiver): FutureIface = {

    Thrift.newMethodIface(
      filterServiceIface(
        serviceIface = serviceIface,
        filters = filter))
  }

  @Provides
  @NonFiltered
  @Singleton
  def providesUnfilteredServiceIface(
    clientId: ClientId,
    statsReceiver: StatsReceiver): ServiceIface = {
    if (mux)
      ThriftMux.client.
        configured(TimeoutFilter.Param(requestTimeout.toTwitterDuration)).
        configured(TimeoutFactory.Param(connectTimeout.toTwitterDuration)).
        configured(Stats(statsReceiver.scope("clnt"))).
        withClientId(clientId).
        newServiceIface[ServiceIface](dest, "")
    else
      Thrift.client.
        configured(TimeoutFilter.Param(requestTimeout.toTwitterDuration)).
        configured(TimeoutFactory.Param(connectTimeout.toTwitterDuration)).
        configured(Stats(statsReceiver.scope("clnt"))).
        withClientId(clientId).
        newServiceIface[ServiceIface](dest, "")
  }

  /* Common Retry Functions */

  lazy val NonCancelledExceptions: PartialFunction[Try[ThriftResponse[_]], Boolean] = {
    case Throw(t) => nonCancelled(t)
    case Return(response) => response.firstException exists nonCancelled
  }

  def nonCancelled(t: Throwable): Boolean = {
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