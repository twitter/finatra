package com.twitter.inject.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{BackupRequestLost, CancelledConnectionException, CancelledRequestException, Failure}
import com.twitter.scrooge.ThriftResponse
import com.twitter.util.{NonFatal, Return, Throw, Try}

/**
 * PossiblyRetryable attempts to determine if a request is possibly retryable based on the
 * returned [[com.twitter.scrooge.ThriftResponse]].
 *
 * The [[com.twitter.scrooge.ThriftResponse#successField]] = None is a possibility on successful
 * void methods. We only want to mark a request as "possibly retryable" if the successField is empty
 * and there exists a "possibly retryable" exception in the first position in the returned Seq of
 * exceptions in [[com.twitter.scrooge.ThriftResponse#errorFields]].
 *
 * @see [[com.twitter.scrooge.ThriftResponse#firstException]]
 *
 * The request is "possibly retryable" because while the framework can say the request is retryable
 * due to the type of [[com.twitter.scrooge.ThriftException]] returned it is ultimately up to the
 * application to decide if the returned [[com.twitter.scrooge.ThriftException]] actually makes sense
 * to be retried.
 */
object PossiblyRetryable {

  val PossiblyRetryableExceptions: PartialFunction[Try[ThriftResponse[_]], Boolean] = {
    case Throw(t) => possiblyRetryable(t)
    case Return(response) =>
      response.successField.isEmpty &&
        response.exceptionFields.nonEmpty &&
          response.firstException().exists(possiblyRetryable)
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
