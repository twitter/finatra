package com.twitter.inject

import com.twitter.finagle.{BackupRequestLost, CancelledRequestException, Failure}
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.util.{Monitor, RootMonitor => UtilRootMonitor}
import scala.util.control.NonFatal

@deprecated(
  "com.twitter.finagle.util.DefaultMonitor will be implicitly installed which will handle these exceptions",
  "2016-11-1"
)
object RootMonitor extends Monitor with Logging {
  override def handle(exc: Throwable): Boolean = exc match {
    case _: CancelledRequestException => true // suppress logging
    case _: com.twitter.util.TimeoutException => true // suppress logging
    case _: com.twitter.finagle.TimeoutException => true // suppress logging
    case _: com.twitter.finagle.mux.ServerApplicationError => true // suppress logging
    case e: Failure if e.isFlagged(Failure.Restartable) => true // suppress logging
    case BackupRequestLost => true // suppress logging
    case NonFatal(e) =>
      warn("Exception propagated to the root monitor: " + ExceptionUtils.toExceptionMessage(e))
      true
    case _ =>
      UtilRootMonitor.handle(exc)
  }
}
