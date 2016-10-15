package com.twitter.inject.thrift.internal.filters

import com.twitter.finagle.{WriteException, BackupRequestLost, Service, SimpleFilter}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{Throw, Try, Stopwatch, Future}
import java.util.concurrent.TimeUnit

private[thrift] class LatencyFilter[Req, Rep](
  statsReceiver: StatsReceiver,
  statName: String = "latency",
  timeUnit: TimeUnit = TimeUnit.MILLISECONDS)
  extends SimpleFilter[Req, Rep] {

  private val latencyStat = statsReceiver.stat(s"${statName}_$latencyStatSuffix")

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    val elapsed = Stopwatch.start()

    service(request).respond { response =>
      if (!isBlackHoleResponse(response)) {
        latencyStat.add(elapsed().inUnit(timeUnit))
      }
    }
  }

  /* Private */

  // Based on `c.t.finagle.service.StatsFilter#isBlackholeResponse`
  private def isBlackHoleResponse(rep: Try[Rep]): Boolean = rep match {
    case Throw(BackupRequestLost) | Throw(WriteException(BackupRequestLost)) =>
      // We blackhole this request. It doesn't count for anything.
      // After the Failure() patch, this should no longer need to
      // be a special case.
      //
      // In theory, we should probably unwind the whole cause
      // chain to look for a BackupRequestLost, but in practice it
      // is wrapped only once.
      true
    case _ =>
      false
  }

  // Based on `c.t.finagle.service.StatsFilter#latencyStatSuffix`
  private def latencyStatSuffix: String = {
    timeUnit match {
      case TimeUnit.NANOSECONDS => "ns"
      case TimeUnit.MICROSECONDS => "us"
      case TimeUnit.MILLISECONDS => "ms"
      case TimeUnit.SECONDS => "secs"
      case _ => timeUnit.toString.toLowerCase
    }
  }
}
