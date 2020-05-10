package com.twitter.inject.utils

import com.twitter.finagle.service.RetryPolicy
import com.twitter.inject.Logging
import com.twitter.inject.utils.FutureUtils._
import com.twitter.util._

object RetryUtils extends Logging {

  /* Public */

  def retryFuture[T](
    retryPolicy: RetryPolicy[Try[T]],
    suppress: Boolean = false
  )(
    func: => Future[T]
  ): Future[T] = {
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

  private def retryMsg[T](sleepTime: Duration, result: Try[T], suppress: Boolean): Unit = {
    if (!suppress) {
      warn("Retrying " + result + " after " + sleepTime)
    }
  }
}
