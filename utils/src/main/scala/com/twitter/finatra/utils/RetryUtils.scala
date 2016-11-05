package com.twitter.finatra.utils

import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.utils.FutureUtils._
import com.twitter.inject.{Logging => NewLogging}
import com.twitter.util._
import scala.annotation.tailrec

object RetryUtils extends NewLogging {

  /* Public */

  @tailrec
  def retry[T](retryPolicy: RetryPolicy[Try[T]], suppress: Boolean = false)(func: => T): Try[T] = {
    val result = Try(func)
    retryPolicy(result) match {
      case Some((sleepTime, nextPolicy)) =>
        Thread.sleep(sleepTime.inMillis)
        retryMsg(sleepTime, result, suppress)
        retry(nextPolicy, suppress)(func)
      case None =>
        result
    }
  }

  def retryFuture[T](retryPolicy: RetryPolicy[Try[T]], suppress: Boolean = false)(func: => Future[T]): Future[T] = {
    exceptionsToFailedFuture(func) transform { result =>
      retryPolicy(result) match {
        case Some((sleepTime, nextPolicy)) =>
          scheduleFuture(sleepTime) {
            retryMsg(sleepTime, result, suppress)
            retryFuture(nextPolicy, suppress)(func)
          }
        case None =>
          Future.const(result)
      }
    }
  }

  /* Private */

  private def retryMsg[T](sleepTime: Duration, result: Try[T], suppress: Boolean) {
    if (!suppress) {
      warn("Retrying " + result + " after " + sleepTime)
    }
  }
}