package com.twitter.finatra.exceptions

import com.twitter.finagle._
import java.net.ConnectException
import org.apache.thrift.transport.TTransportException

/** Exceptions caused by requests to external services */
@deprecated("Use ThriftClientExceptionMapper with FilteredThriftClientModule", "2015-11-04")
object ExternalServiceExceptionMatcher {
  def apply(t: Throwable): Boolean = t match {
    case _: RequestException => true
    case _: ApiException => true
    case _: WriteException => true
    case _: com.twitter.util.TimeoutException => true
    case _: TimeoutException => true
    case _: ChannelClosedException => true
    case _: InterruptedException => true
    case _: TTransportException => true
    case _: ConnectException => true
    case _ => false
  }

  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}
