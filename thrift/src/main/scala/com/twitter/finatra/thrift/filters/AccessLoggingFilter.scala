package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.thrift.{ClientId, MethodMetadata}
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Throw, Time}
import java.util.TimeZone
import javax.inject.Singleton
import org.apache.commons.lang.time.FastDateFormat

/**
 * AccessLoggingFilter attempts to follow the Common Log Format as closely as
 * possible.
 * @see https://en.wikipedia.org/wiki/Common_Log_Format
 */
@Singleton
class AccessLoggingFilter extends Filter.TypeAgnostic with Logging {
  private val DateFormat =
    FastDateFormat.getInstance("dd/MMM/yyyy:HH:mm:ss Z", TimeZone.getTimeZone("GMT"))

  /* Public */

  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = {
      val start = Time.now
      service(request).respond {
        case Return(_) =>
          info(prelog(start, request))
        case Throw(t) =>
          warn(prelog(start, request) + " failure " + t.getClass.getSimpleName)
      }
    }

    /* Private */
    private[this] def prelog(start: Time, request: T): String = {
      val elapsed = start.untilNow.inMilliseconds
      val startStr = DateFormat.format(start.toDate)
      val clientIdStr = ClientId.current.map(_.name).getOrElse("-")
      val methodStr = MethodMetadata.current.map(_.methodName).getOrElse("-")
      s"$clientIdStr $startStr '$methodStr' $elapsed"
    }
  }
}
