package com.twitter.finatra.thrift.filters

import com.twitter.finagle.stats.{Counter, StatsReceiver}
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

@Singleton
class ClientIdWhitelistFilter @Inject()(
  whitelist: Set[ClientId],
  statsReceiver: StatsReceiver)
  extends ThriftFilter {

  private val clientRequestStats = statsReceiver.scope("client_id_whitelist")

  private val unknownStats = clientRequestStats.scope("unknown_client_id")
  private val unknownCounter = clientRequestStats.counter("unknown_client_id")
  private val noClientIdStats = clientRequestStats.scope("no_client_id")
  private val noClientIdCounter = clientRequestStats.counter("no_client_id")

  private val unknownClientIdException = Future.exception(new UnknownClientIdError("unknown client id"))
  private val noClientIdException = Future.exception(new NoClientIdError("The request did not contain a Thrift client id"))

  /* Public */

  override def apply[T, U](request: ThriftRequest[T], service: Service[ThriftRequest[T], U]): Future[U] = {
    request.clientId match {
      case None =>
        incrementStats(noClientIdStats, noClientIdCounter, request)
        noClientIdException
      case Some(clientId) if whitelist.contains(clientId) =>
        service(request)
      case _ =>
        incrementStats(unknownStats, unknownCounter, request)
        unknownClientIdException
    }
  }

  /* Private */

  private def incrementStats[T](scopedStats: StatsReceiver, counter: Counter, request: ThriftRequest[T]): Unit = {
    counter.incr()

    for (clientId <- request.clientId) {
      scopedStats.scope(request.methodName).counter(clientId.name).incr()
    }
  }
}

