package com.twitter.inject.thrift

import com.github.nscala_time.time
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.factory.TimeoutFactory
import com.twitter.finagle.param.{Label, Stats}
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{ClientId, ServiceIfaceBuilder}
import com.twitter.inject.TwitterModule
import com.twitter.inject.thrift.FilteredThriftClientModule.MaxDuration
import com.twitter.inject.thrift.conversions.DurationConversions
import com.twitter.scrooge.{ThriftResponse, ThriftService}
import com.twitter.util.{NonFatal, Return, Throw, Try}
import javax.inject.Singleton
import org.joda.time.Duration
import scala.reflect.ClassTag

object FilteredThriftClientModule {
  val MaxDuration = Duration.millis(Long.MaxValue)
}

abstract class FilteredThriftClientModule[FutureIface <: ThriftService : ClassTag, ServiceIface: ClassTag](
  implicit builder: ServiceIfaceBuilder[ServiceIface])
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

  def params = {
    Stack.Params.empty +
      TimeoutFilter.Param(requestTimeout.toTwitterDuration) +
      TimeoutFactory.Param(connectTimeout.toTwitterDuration) +
      Label(label)
  }

  /**
   * Add filters to the ServiceIface based client
   */
  def createFilteredClient(
    serviceIface: ServiceIface,
    filters: FilterBuilder): FutureIface

  @Provides
  @Singleton
  def providesClient(
    @NonFiltered serviceIface: ServiceIface,
    filter: FilterBuilder,
    statsReceiver: StatsReceiver): FutureIface = {

    createFilteredClient(
      serviceIface = serviceIface,
      filters = filter)
  }

  @Provides
  @NonFiltered
  @Singleton
  def providesUnfilteredServiceIface(clientId: ClientId, statsReceiver: StatsReceiver): ServiceIface = {
    val updatedParams = params +
      Stats(statsReceiver.scope("clnt")) +
      Thrift.param.ClientId(Some(clientId))

    if (mux)
      ThriftMux.client.
        withParams(updatedParams).
        newServiceIface[ServiceIface](dest)
    else
      Thrift.client.
        withParams(updatedParams).
        newServiceIface[ServiceIface](dest)
  }

  /* Common Retry Functions */

  lazy val NonFatalExceptions: PartialFunction[Try[ThriftResponse[_]], Boolean] = {
    case Throw(NonFatal(_)) => true
    case Return(result) => result.firstException exists NonFatal.isNonFatal
  }
}