package com.twitter.finatra.http.filters

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.{Counter, Stat, StatsReceiver}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.filters.StatsFilter.Stats
import com.twitter.finatra.http.internal.contexts.RouteInfo
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.inject.Logging
import com.twitter.util.{Duration, Future, Memoize, Stopwatch, Return, Throw}
import javax.inject.{Inject, Singleton}

object StatsFilter {
  private object Stats {
    def mk(statsReceiver: StatsReceiver, statusCode: Int): Stats = {
      val statusClass = s"${statusCode / 100}XX"
      Stats(
        requestCount = statsReceiver.counter("requests"),
        statusCodeCount = statsReceiver.scope("status").counter(statusCode.toString),
        statusClassCount = statsReceiver.scope("status").counter(statusClass),
        statusCodeTime = statsReceiver.scope("time").stat(statusCode.toString),
        statusClassTime = statsReceiver.scope("time").stat(statusClass),
        responseSize = statsReceiver.stat("response_size"))
    }
  }

  private case class Stats(
    requestCount: Counter,
    statusCodeCount: Counter,
    statusClassCount: Counter,
    statusCodeTime: Stat,
    statusClassTime: Stat,
    responseSize: Stat
  ) {
    def count(duration: Duration, response: Response, totalRequests: Boolean): Unit = {
      if (totalRequests) {
        requestCount.incr()
      }
      statusCodeCount.incr()
      statusClassCount.incr()
      statusCodeTime.add(duration.inMilliseconds)
      statusClassTime.add(duration.inMilliseconds)
      responseSize.add(response.length)
    }
  }
}

/**
 * A drop-in replacement for [[com.twitter.finagle.http.filter.StatsFilter]]
 * with per-route stats scoped under `route/<name>/<method>`.
 */
@Singleton
class StatsFilter[R <: Request] @Inject()(
  statsReceiver: StatsReceiver)
  extends SimpleFilter[R, Response]
  with Logging {

  private val perRouteStats = Memoize[(RouteInfo, Int), Stats] {
    case (RouteInfo(name, method), statusCode) =>
      val scopedStatsReceiver =
        statsReceiver.
          scope("route").
          scope(name).
          scope(method.getName)
      Stats.mk(scopedStatsReceiver, statusCode)
  }

  private val globalStats = Memoize[Int, Stats] { statusCode =>
    Stats.mk(statsReceiver, statusCode)
  }

  /* Public */

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    val elapsed = Stopwatch.start()
    service(request) respond {
      case Return(response) =>
        record(request, response, elapsed())
      case Throw(e) =>
        error(s"Internal server error - please ensure ${classOf[ExceptionMappingFilter[_]].getName} is installed", e)
        record(request, SimpleResponse(Status.InternalServerError), elapsed())
    }
  }

  /* Private */

  private def record(request: Request, response: Response, duration: Duration): Unit = {
    globalStats(response.statusCode).count(duration, response, totalRequests = false)
    RouteInfo(request) foreach { info =>
      perRouteStats(info, response.statusCode).count(duration, response, totalRequests = true)
    }
  }
}
