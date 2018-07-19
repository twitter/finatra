package com.twitter.inject.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{BackupRequestLost, CancelledConnectionException, CancelledRequestException, Failure, FailureFlags, service => ctfs}
import com.twitter.finagle.service.{ReqRep, ResponseClass}
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

  /**
   * Partial function which can be used to determine if a request is possibly
   * retryable. "Possibly" retryable means the returned exception is '''not'''
   * a cancellation, does '''not''' represent an exception of failure that should
   * not be tried and lastly is non-fatal.
   */
  val PossiblyRetryableExceptions: PartialFunction[Try[_], Boolean] = {
    case Return(_) => false
    case Throw(t) => possiblyRetryable(t)
  }

  /**
   * A [[com.twitter.finagle.service.ResponseClassifier]] which uses the
   * [[PossiblyRetryableExceptions]] partial function to classify responses as
   * retryable.
   */
  val ResponseClassifier: ctfs.ResponseClassifier =
    ctfs.ResponseClassifier.named("PossiblyRetryableExceptions") {
      case ReqRep(_, Throw(t)) if PossiblyRetryable.possiblyRetryable(t) =>
        ResponseClass.RetryableFailure
      case ReqRep(_, Throw(_)) =>
        ResponseClass.NonRetryableFailure
      case ReqRep(_, Return(_)) =>
        ResponseClass.Success
    }

  def apply(t: Throwable): Boolean = {
    PossiblyRetryable.possiblyRetryable(t)
  }

  def unapply(t: Throwable): Option[Throwable] =
    if (apply(t)) Some(t) else None

  /**
   * A [[Throwable]] is "possibly" retryable if:
   *  a. it is not a cancellation
   *  b. it is not an exception or failure which should not be retried
   *  c. is a non-fatal exception
   * @param t the [[Throwable]] to inspect
   * @return true if the [[Throwable]] represents an Exception or Failure which is possibly
   *         retryable, false otherwise.
   */
  def possiblyRetryable(t: Throwable): Boolean = {
    !isCancellation(t) && !isNonRetryable(t) && NonFatal(t)
  }

  private[inject] def isCancellation(t: Throwable): Boolean = t match {
    case BackupRequestLost => true
    case _: CancelledRequestException => true
    case _: CancelledConnectionException => true
    case _: ClientDiscardedRequestException => true
    case f: Failure if f.isFlagged(FailureFlags.Interrupted) => true
    case f: Failure if f.cause.isDefined => isCancellation(f.cause.get)
    case _ => false
  }

  private[inject] def isNonRetryable(t: Throwable) : Boolean = t match {
    case BackupRequestLost => true
    case _: NonRetryableException => true
    case f: Failure if f.isFlagged(FailureFlags.Ignorable) => true
    case f: Failure if f.cause.isDefined => isNonRetryable(f.cause.get)
    case _ => false
  }
}
