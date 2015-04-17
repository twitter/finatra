package com.twitter.finatra.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.Service
import com.twitter.finatra.internal.exceptions.ExceptionManager
import com.twitter.util.Memoize
import javax.inject.{Inject, Singleton}

@Singleton
@deprecated("Use ExceptionMapperFilter with com.twitter.finagle.http.filter.StatsFilter[Request] & StatsFilterModule", "")
class ExceptionBarrierFilter @Inject()(
  statsReceiver: StatsReceiver,
  exceptionManager: ExceptionManager)
  extends ExceptionMappingFilter(exceptionManager) {

  private val responseCodeStatsReceiver = statsReceiver.scope("server/response/status")

  /* Public */

  override def apply(request: Request, service: Service[Request, Response]) = {
    super.apply(request, service) onSuccess { response =>
      statusCodeCounter(response.status.getCode).incr()
    }
  }

  /* Private */

  private val statusCodeCounter = Memoize { statusCode: Int =>
    responseCodeStatsReceiver.counter(statusCode.toString)
  }
}
