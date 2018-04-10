package com.twitter.finatra.http.filters

import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Stopwatch, Throw}
import javax.inject.{Inject, Singleton}

/**
 * Provides a standard "Access Log" -- a list of all requests through this Filter. Typically, this
 * Filter is provided by the [[com.twitter.finatra.http.modules.AccessLogModule]] which provides an
 * implementation for the [[LogFormatter]] param as an instance of
 * [[com.twitter.finagle.http.filter.CommonLogFormatter]].
 *
 * Usage: To use, configure a logger (with your preferred logging implementation) over this class
 * that writes to a specific file (usually named, `access.log`).
 *
 * @note This Filter '''should''' occur as early in the Filter chain as possible such that it is
 *       "above" the [[ExceptionMappingFilter]] as it is expected that servicing requests with this
 *       Filter will ''always'' return a [[com.twitter.finagle.http.Response]].
 * @note This Filter is included in the Finatra [[com.twitter.finatra.http.filters.CommonFilters]].
 *
 * @param logFormatter an instance of [[LogFormatter]] which specifies how log lines should be formatted.
 * @tparam R - "Request" type param which must be a subtype of [[com.twitter.finagle.http.Request]].
 *
 * @see [[com.twitter.finatra.http.filters.CommonFilters]]
 * @see [[com.twitter.finatra.http.filters.ExceptionMappingFilter]]
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
      service(request).respond {
        case Return(response) =>
          info(logFormatter.format(request, response, elapsed()))
        case Throw(e) =>
          // should never get here since this filter is meant to be after (above)
          // the `ExceptionMappingFilter` which translates exceptions to responses.
          info(logFormatter.formatException(request, e, elapsed()))
      }
    }
  }
}
