package com.twitter.finatra.thrift.filters

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.context.{Contexts, RemoteInfo}
import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.thrift.{ClientId, MethodMetadata}
import com.twitter.util.{Duration, ScheduledThreadPoolTimer}
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.Locale

private object ThriftCommonLogFormatter {
  val DateFormat: DateTimeFormatter = {
    DateTimeFormatter
      .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
      .withLocale(Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"))
  }
}

/**
 * A CommonLogFormatter for formatting ThriftMux requests.
 *
 * @see [[https://en.wikipedia.org/wiki/Common_Log_Format]]
 */
private[thrift] final class ThriftCommonLogFormatter extends LogFormatter[Any, Any] {
  import ThriftCommonLogFormatter._

  // optimized
  @volatile private var currentDateValue: String = getCurrentDateValue
  new ScheduledThreadPoolTimer(poolSize = 1, name = "ThriftDateUpdater", makeDaemons = true)
    .schedule(1.second) {
      currentDateValue = getCurrentDateValue
    }

  private[this] def getCurrentDateValue: String = ZonedDateTime.now.format(DateFormat)

  def format(request: Any, response: Any, responseTime: Duration): String = {
    val remoteAddr =
      Contexts.local.get(RemoteInfo.Upstream.AddressCtx) match {
        case Some(addr) =>
          val asString = addr.toString
          if (asString.startsWith("/")) asString.substring(1)
          else asString
        case _ => ""
      }
    val clientId = ClientId.current match {
      case Some(client) => client.name
      case _ => ""
    }
    val (methodName, serviceName) = MethodMetadata.current match {
      case Some(method) => (method.methodName, method.serviceName)
      case _ => ("", "")
    }

    val builder = new StringBuilder(256)
    builder.append(remoteAddr)
    builder.append(" - - [")
    builder.append(currentDateValue)
    builder.append("] \"")
    builder.append(clientId)
    builder.append(' ')
    builder.append(serviceName)
    builder.append(' ')
    builder.append(methodName)
    builder.append("\" ")
    response match {
      case throwable: Throwable =>
        builder.append(s"failure ${throwable.getClass.getSimpleName}")
        builder.append(' ')
      case _ =>
      // do nothing
    }
    if (response != null && response.isInstanceOf[Array[Byte]]) {
      builder.append(response.asInstanceOf[Array[Byte]].length)
      builder.append(' ')
    } else {
      builder.append("- ")
    }

    builder.append(responseTime.inMillis)

    builder.toString
  }

  def formatException(request: Any, throwable: Throwable, responseTime: Duration): String =
    format(request, throwable, responseTime)
}
