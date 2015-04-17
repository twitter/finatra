package com.twitter.finatra.filters

import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.Logging
import com.twitter.util.{Future, Stopwatch}
import javax.inject.Inject

class AccessLoggingFilter @Inject()(
  logFormatter: LogFormatter[Request, Response])
  extends SimpleFilter[Request, Response]
  with Logging {

  //optimized
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    if (!isInfoEnabled) {
      service(request)
    }
    else {
      val elapsed = Stopwatch.start()
      service(request) onSuccess { response =>
        info(logFormatter.format(request, response, elapsed()))
      } onFailure { e =>
        // should never get here since this filter is meant to be after the exception barrier
        info(logFormatter.formatException(request, e, elapsed()))
      }
    }
  }
}