package com.twitter.finatra.thrift.filters

import com.twitter.finagle.stats.Stat.timeFuture
import com.twitter.finagle.stats.{Counter, Stat, StatsReceiver}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.util.{Future, Memoize, Return, Throw}
import javax.inject.{Inject, Singleton}

@Singleton
class StatsFilter @Inject()(
  statsReceiver: StatsReceiver)
  extends SimpleFilter[ThriftRequest, Any] {

  private val requestStats = statsReceiver.scope("per_method_stats")
  private val exceptionCounter = statsReceiver.counter("exceptions")
  private val exceptionStatsReceiver = statsReceiver.scope("exceptions")

  override def apply(request: ThriftRequest, service: Service[ThriftRequest, Any]): Future[Any] = {
    val methodStats = lookupThriftMethodStats(request.methodName)

    timeFuture(methodStats.latencyStat) {
      service(request)
    } respond {
      case Return(_) =>
        methodStats.successCounter.incr()
      case Throw(t) =>
        methodStats.failuresCounter.incr()
        methodStats.failuresScope.counter(t.getClass.getName).incr()
        exceptionCounter.incr()
        exceptionStatsReceiver.counter(t.getClass.getName).incr()
    }
  }

  private val lookupThriftMethodStats = Memoize { methodName: String =>
    ThriftMethodStats(
      requestStats.scope(methodName))
  }

  object ThriftMethodStats {
    def apply(stats: StatsReceiver): ThriftMethodStats =
      ThriftMethodStats(
        latencyStat = stats.stat("latency_ms"),
        successCounter = stats.counter("success"),
        failuresCounter = stats.counter("failures"),
        failuresScope = stats.scope("failures"))
  }

  case class ThriftMethodStats(
    latencyStat: Stat,
    successCounter: Counter,
    failuresCounter: Counter,
    failuresScope: StatsReceiver)
}
