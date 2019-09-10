package com.twitter.finatra.thrift.filters

import com.twitter.finagle.thrift.{ClientId, MethodMetadata}
import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Throw, Time, TwitterDateFormat}
import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}
import javax.inject.Singleton

/**
 * AccessLoggingFilter attempts to follow the Common Log Format as closely as
 * possible.
 * @see https://en.wikipedia.org/wiki/Common_Log_Format
 */
@Singleton
class AccessLoggingFilter extends Filter.TypeAgnostic with Logging {
  import AccessLoggingFilter._

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
      val startStr = DateFormat.get().format(start.toDate)
      val clientIdStr = ClientId.current.map(_.name).getOrElse("-")
      val methodStr = MethodMetadata.current.map(_.methodName).getOrElse("-")
      s"$clientIdStr $startStr '$methodStr' $elapsed"
    }
  }
}

object AccessLoggingFilter {
  private val DateFormat = new ThreadLocal[SimpleDateFormat] {
    override protected def initialValue(): SimpleDateFormat = {
      val f = TwitterDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH)
      f.setTimeZone(TimeZone.getTimeZone("GMT"))
      f
    }
  }
}
