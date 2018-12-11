package com.twitter.finatra.thrift.filters

import com.twitter.finagle.stats.{Counter, StatsReceiver}
import com.twitter.finagle.thrift.{ClientId, MethodMetadata}
import com.twitter.finagle.{Filter, Service}
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
  extends Filter.TypeAgnostic {
  import com.twitter.finatra.thrift.thriftscala
  import ClientIdAcceptlistFilter._

  private[this] val clientRequestStats = statsReceiver.scope(StatsScope)

  protected[this] val unknownStats: StatsReceiver = clientRequestStats.scope("unknown_client_id")
  protected[this] val unknownCounter: Counter = clientRequestStats.counter("unknown_client_id")
  protected[this] val noClientIdStats: StatsReceiver = clientRequestStats.scope("no_client_id")
  protected[this] val noClientIdCounter: Counter = clientRequestStats.counter("no_client_id")

  protected val unknownClientIdException =
    Future.exception(new thriftscala.UnknownClientIdError("unknown client id"))
  protected val noClientIdException =
    Future.exception(new thriftscala.NoClientIdError("The request did not contain a Thrift client id"))


  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = {
      ClientId.current match {
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

    protected[this] def incrementStats(
      scopedStats: StatsReceiver,
      counter: Counter,
      request: T
    ): Unit = {
      counter.incr()

      for (clientId <- ClientId.current) {
        val scope = MethodMetadata.current match {
          case Some(m) => scopedStats.scope(m.methodName)
          case None => scopedStats
        }
        scope.counter(clientId.name).incr()
      }
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

  override protected val unknownClientIdException =
    Future.exception(new thriftjava.UnknownClientIdError("unknown client id"))
  override protected val noClientIdException =
    Future.exception(new thriftjava.NoClientIdError("The request did not contain a Thrift client id"))
}
