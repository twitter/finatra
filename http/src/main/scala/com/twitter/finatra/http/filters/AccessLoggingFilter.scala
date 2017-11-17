package com.twitter.finatra.http.filters

import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.Logging
import com.twitter.util.{Future, Stopwatch}
import javax.inject.{Inject, Singleton}

/**
 * Provides a standard "Access Log" -- a list of all requests through this Filter.
 *
 * NOTE: This Filter should occur as early in the Filter chain as possible.
 * Typically, this Filter is provided by the [[com.twitter.finatra.http.modules.AccessLogModule]]
 * which provides an implementation for the [[LogFormatter]] param as an instance of
 * [[com.twitter.finagle.http.filter.CommonLogFormatter]].
 *
 * This Filter is included in the Finatra [[com.twitter.finatra.http.filters.CommonFilters]].
 *
 * Usage: To use, create a logger (with your preferred logging implementation) over this class
 * which is configured to write to a specific file (usually named, `access.log`).
 *
 * @param logFormatter an instance of [[LogFormatter]] which specifies how log lines should be formatted.
 * @tparam R - "Request" type param which must be a subtype of [[com.twitter.finagle.http.Request]].
 *
 * @see [[com.twitter.finatra.http.filters.CommonFilters]]
 * @see [[com.twitter.finatra.http.modules.AccessLogModule]]
 * @see [[com.twitter.finagle.filter.LogFormatter]]
 * @see [[com.twitter.finagle.http.filter.CommonLogFormatter]]
 */
@Singleton
class AccessLoggingFilter[R <: Request] @Inject()(logFormatter: LogFormatter[R, Response])
    extends SimpleFilter[R, Response]
    with Logging {

  //optimized
  override def apply(request: R, service: Service[R, Response]): Future[Response] = {
    if (!isInfoEnabled) {
      service(request)
    } else {
      val elapsed = Stopwatch.start()
      service(request) onSuccess { response =>
        info(logFormatter.format(request, response, elapsed()))
      } onFailure { e =>
        // should never get here since this filter is meant to be after the `ExceptionMappingFilter`
        info(logFormatter.formatException(request, e, elapsed()))
      }
    }
  }
}
