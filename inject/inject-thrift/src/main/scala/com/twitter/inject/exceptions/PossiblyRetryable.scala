package com.twitter.inject.exceptions

import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.finagle.{BackupRequestLost, CancelledConnectionException, CancelledRequestException, Failure, service => ctfs}
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
   * retryable.
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

  def unapply(t: Throwable): Option[Throwable] = Some(t).filter(apply)

  // TODO: RetryableWriteException's are automatically retried by Finagle, so we should avoid retrying again here
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
    case f: Failure if f.isFlagged(Failure.Interrupted) || f.isFlagged(Failure.Ignorable) => true
    case f: Failure if f.cause.isDefined => isCancellation(f.cause.get)
    case _ => false
  }
}
