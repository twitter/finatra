package com.twitter.finatra.http.filters

import com.twitter.finagle.Service
import com.twitter.finagle.SimpleFilter
import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Stopwatch
import com.twitter.util.Throw
import com.twitter.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

private object AccessLoggingFilter {
  val logger: Logger = Logger(AccessLoggingFilter.getClass)
}

/**
 * Provides a standard "Access Log" -- a list of all requests through this Filter. Typically, this
 * Filter is provided by the [[com.twitter.finatra.http.modules.AccessLogModule]] which provides an
 * implementation for the [[com.twitter.finagle.filter.LogFormatter]] param as an instance of
 * [[com.twitter.finagle.http.filter.CommonLogFormatter]].
 *
 * =Usage=
 * To use, configure a logger (with your preferred logging implementation) over this class
 * which writes to a specific file (typically named, `access.log`).
 *
 * @note This Filter '''should''' occur as early in the Filter chain as possible such that it is
 *       "above" the [[com.twitter.finatra.http.filters.ExceptionMappingFilter]] as it is expected
 *       that servicing requests with this Filter will ''always'' return a [[com.twitter.finagle.http.Response]].
 * @note This Filter is included in the Finatra [[com.twitter.finatra.http.filters.CommonFilters]].
 *
 * @param formatter an instance of [[com.twitter.finagle.filter.LogFormatter]] which specifies how
 *                  log lines should be formatted.
 * @tparam R - "Request" type param which must be a subtype of [[com.twitter.finagle.http.Request]].
 *
 * @see [[https://en.wikipedia.org/wiki/Common_Log_Format Common Log Format]]
 * @see [[com.twitter.finatra.http.filters.CommonFilters]]
 * @see [[com.twitter.finatra.http.filters.ExceptionMappingFilter]]
 * @see [[com.twitter.finatra.http.modules.AccessLogModule]]
 * @see [[com.twitter.finagle.filter.LogFormatter]]
 * @see [[com.twitter.finagle.http.filter.CommonLogFormatter]]
 */
@Singleton
class AccessLoggingFilter[R <: Request] @Inject() (formatter: LogFormatter[R, Response])
    extends SimpleFilter[R, Response] {
  import AccessLoggingFilter._

  override def apply(request: R, service: Service[R, Response]): Future[Response] = {
    val elapsed = Stopwatch.start()
    service(request).respond {
      case Return(response) =>
        logger.info(formatter.format(request, response, elapsed()))
      case Throw(e) =>
        // should never get here since this filter is meant to be after (above)
        // the `ExceptionMappingFilter` which translates exceptions to responses.
        logger.warn(formatter.formatException(request, e, elapsed()))
    }
  }
}
