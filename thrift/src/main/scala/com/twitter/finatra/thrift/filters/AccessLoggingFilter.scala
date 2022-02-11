package com.twitter.finatra.thrift.filters

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.finagle.filter.LogFormatter
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Stopwatch
import com.twitter.util.Throw
import com.twitter.util.logging.Logger
import javax.inject.Singleton

private object AccessLoggingFilter {
  val logger: Logger = Logger(AccessLoggingFilter.getClass)
}

/**
 * Provides a standard "Access Log" -- a list of all requests through this Filter. The Filter uses an
 * implementation of the [[com.twitter.finagle.filter.LogFormatter]], [[ThriftCommonLogFormatter]],
 * which attempts to follow the [[https://en.wikipedia.org/wiki/Common_Log_Format Common Log Format]]
 * as closely as possible.
 *
 * =Usage=
 * To use, configure a logger (with your preferred logging implementation) over this class
 * which writes to a specific file (typically named, `access.log`).
 *
 * @note This Filter '''should''' occur as early in the Filter chain as possible such that it is
 *       "above" the [[com.twitter.finatra.thrift.filters.ExceptionMappingFilter]].
 *
 * @see [[https://en.wikipedia.org/wiki/Common_Log_Format Common Log Format]]
 * @see [[com.twitter.finatra.thrift.filters.ExceptionMappingFilter]]
 * @see [[com.twitter.finagle.filter.LogFormatter]]
 */
@Singleton
class AccessLoggingFilter extends Filter.TypeAgnostic {
  import AccessLoggingFilter._

  private[this] val formatter: LogFormatter[Any, Any] = new ThriftCommonLogFormatter

  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(request: T, service: Service[T, U]): Future[U] = {
      val elapsed = Stopwatch.start()
      service(request).respond {
        case Return(response) =>
          logger.info(formatter.format(request, response, elapsed()))
        case Throw(e) =>
          logger.warn(formatter.formatException(request, e, elapsed()))
      }
    }
  }
}
