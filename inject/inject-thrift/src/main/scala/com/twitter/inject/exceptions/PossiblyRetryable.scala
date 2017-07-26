package com.twitter.inject.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{
  BackupRequestLost,
  CancelledConnectionException,
  CancelledRequestException,
  Failure
}
import com.twitter.util.{Return, Throw, Try}
import scala.util.control.NonFatal

/**
 * PossiblyRetryable attempts to determine if a request is possibly retryable based on the
 * returned `Try`.
 *
 * The request is "possibly retryable" because while the framework can say the request is retryable
 * due to the type of [[com.twitter.scrooge.ThriftException]] returned it is ultimately up to the
 * application to decide if the returned [[com.twitter.scrooge.ThriftException]] actually makes sense
 * to be retried.
 */
object PossiblyRetryable {

  val PossiblyRetryableExceptions: PartialFunction[Try[_], Boolean] = {
    case Return(_) => false
    case Throw(t) => possiblyRetryable(t)
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
    NonFatal(t)
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
