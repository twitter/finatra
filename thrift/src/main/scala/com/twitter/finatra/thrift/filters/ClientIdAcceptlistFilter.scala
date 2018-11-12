package com.twitter.finatra.thrift.filters

import com.twitter.finagle.stats.{Counter, StatsReceiver}
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

/** Visible for testing */
object ClientIdAcceptlistFilter {
  val StatsScope = "client_id_whitelist" // TODO: CHANGE SCOPE NAME
}

/**
 * Utility to reject requests from unknown clients. This Filter returns generated Scala errors
 * defined in the `finatra-thrift/finatra_thrift_exceptions.thrift` IDL.
 *
 * @note This is only applicable in Scala since the [[ClientIdAcceptlistFilter]] uses
 *       generated Scala classes for the exceptions. Java users should use the [[JavaClientIdAcceptlistFilter]].
 * @see `finatra-thrift/finatra_thrift_exceptions.thrift`
 */
@Singleton
class ClientIdAcceptlistFilter @Inject()(
  acceptList: Set[ClientId],
  statsReceiver: StatsReceiver)
  extends ThriftFilter {
  import com.twitter.finatra.thrift.thriftscala
  import ClientIdAcceptlistFilter._

  private[this] val clientRequestStats = statsReceiver.scope(StatsScope)

  protected[this] val unknownStats: StatsReceiver = clientRequestStats.scope("unknown_client_id")
  protected[this] val unknownCounter: Counter = clientRequestStats.counter("unknown_client_id")
  protected[this] val noClientIdStats: StatsReceiver = clientRequestStats.scope("no_client_id")
  protected[this] val noClientIdCounter: Counter = clientRequestStats.counter("no_client_id")

  private[this] val unknownClientIdException =
    Future.exception(new thriftscala.UnknownClientIdError("unknown client id"))
  private[this] val noClientIdException =
    Future.exception(new thriftscala.NoClientIdError("The request did not contain a Thrift client id"))

  /* Public */

  def apply[T, U](
    request: ThriftRequest[T],
    service: Service[ThriftRequest[T], U]
  ): Future[U] = {
    request.clientId match {
      case None =>
        incrementStats(noClientIdStats, noClientIdCounter, request)
        noClientIdException
      case Some(clientId) if acceptList.contains(clientId) =>
        service(request)
      case _ =>
        incrementStats(unknownStats, unknownCounter, request)
        unknownClientIdException
    }
  }

  /* Private */

  protected[this] def incrementStats[T](
    scopedStats: StatsReceiver,
    counter: Counter,
    request: ThriftRequest[T]
  ): Unit = {
    counter.incr()

    for (clientId <- request.clientId) {
      scopedStats.scope(request.methodName).counter(clientId.name).incr()
    }
  }
}

/**
 * Utility to reject requests from unknown clients. This Filter returns generated Java errors
 * defined in the `finatra-thrift/finatra_thrift_exceptions.thrift` IDL.
 *
 * @note This is only applicable in Java since the [[JavaClientIdAcceptlistFilter]] uses
 *       generated Java classes for the exceptions. Scala users should use the [[ClientIdAcceptlistFilter]].
 * @see `finatra-thrift/finatra_thrift_exceptions.thrift`
 */
@Singleton
class JavaClientIdAcceptlistFilter @Inject()(
  acceptList: Set[ClientId],
  statsReceiver: StatsReceiver)
  extends ClientIdAcceptlistFilter(acceptList, statsReceiver) {
  import com.twitter.finatra.thrift.thriftjava

  private[this] val unknownClientIdException =
    Future.exception(new thriftjava.UnknownClientIdError("unknown client id"))
  private[this] val noClientIdException =
    Future.exception(new thriftjava.NoClientIdError("The request did not contain a Thrift client id"))

  /* Public */

  override def apply[T, Rep](
    request: ThriftRequest[T],
    service: Service[ThriftRequest[T], Rep]
  ): Future[Rep] = {
    request.clientId match {
      case None =>
        incrementStats(noClientIdStats, noClientIdCounter, request)
        noClientIdException
      case Some(clientId) if acceptList.contains(clientId) =>
        service(request)
      case _ =>
        incrementStats(unknownStats, unknownCounter, request)
        unknownClientIdException
    }
  }
}
