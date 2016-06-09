package com.twitter.inject.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{BackupRequestLost, CancelledConnectionException, CancelledRequestException, Failure}
import com.twitter.scrooge.ThriftResponse
import com.twitter.util.{NonFatal, Return, Throw, Try}

object PossiblyRetryable {

  val PossiblyRetryableExceptions: PartialFunction[Try[ThriftResponse[_]], Boolean] = {
    case Throw(t) => possiblyRetryable(t)
    case Return(response) => response.exceptionFields.exists(_.forall(possiblyRetryable))
  }

  def apply(t: Throwable): Boolean = {
    PossiblyRetryable.possiblyRetryable(t)
  }

  def unapply(t: Throwable): Option[Throwable] = {
    if (apply(t))
      Some(t)
    else
      None
  }

  // TODO: RetryableWriteException's are automatically retried by Finagle (how many times?), so we should avoid retrying again here
  def possiblyRetryable(t: Throwable): Boolean = {
    !isCancellation(t) &&
      !t.isInstanceOf[NonRetryableException] &&
      NonFatal.isNonFatal(t)
  }

  def isCancellation(t: Throwable): Boolean = t match {
    case BackupRequestLost => true
    case _: CancelledRequestException => true
    case _: CancelledConnectionException => true
    case _: ClientDiscardedRequestException => true
    case f: Failure if f.isFlagged(Failure.Interrupted) => true
    case f: Failure if f.cause.isDefined => isCancellation(f.cause.get)
    case _ => false
  }

}
