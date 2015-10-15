package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.Logging
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.util.{Future, Return, Throw, Time}
import java.util.TimeZone
import javax.inject.Singleton
import org.apache.commons.lang.time.FastDateFormat

@Singleton
class AccessLoggingFilter
  extends SimpleFilter[ThriftRequest, Any]
  with Logging {

  private val DateFormat = FastDateFormat.getInstance("dd/MMM/yyyy:HH:mm:ss Z", TimeZone.getTimeZone("GMT"))

  /* Public */

  override def apply(request: ThriftRequest, service: Service[ThriftRequest, Any]): Future[Any] = {
    val start = Time.now
    service(request).respond {
      case Return(_) =>
        info(prelog(start, request))
      case Throw(t) =>
        warn(prelog(start, request) + " failure " + t.getClass.getSimpleName)
    }
  }

  /* Private */

  private def prelog(start: Time, request: ThriftRequest): String = {
    val elapsed = (Time.now - start).inMilliseconds
    val startStr = DateFormat.format(start.toDate)
    val clientIdStr = request.clientId map { _.name } getOrElse "-"
    s"$clientIdStr $startStr '${request.methodName}' $elapsed"
  }
}
