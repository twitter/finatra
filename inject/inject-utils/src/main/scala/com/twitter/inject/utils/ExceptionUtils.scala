package com.twitter.inject.utils

import com.twitter.finagle.CancelledConnectionException
import com.twitter.finagle.CancelledRequestException
import com.twitter.finagle.Failure
import com.twitter.finagle.FailureFlags
import com.twitter.finagle.FailedFastException
import com.twitter.finagle.SourcedException
import com.twitter.finagle.TimeoutException
import com.twitter.finagle.mux.ClientDiscardedRequestException
import com.twitter.inject.exceptions.NonRetryableException
import com.twitter.util.Throwables._
import com.twitter.util.Throw
import com.twitter.util.Try
import scala.annotation.tailrec

object ExceptionUtils {

  def stripNewlines(e: Throwable): String = {
    stripNewlines(e.toString)
  }

  def stripNewlines(str: String): String = {
    str.replace("\n\twith NoSources", "")
  }

  def toExceptionDetails(exception: Throwable): String = {
    mkString(exception).mkString("/")
  }

  def toExceptionMessage(tryThrowable: Try[_]): String = tryThrowable match {
    case Throw(e) => toExceptionMessage(e)
    case _ => ""
  }

  def toExceptionMessage(exception: Throwable): String = exception match {
    case e: TimeoutException =>
      e.exceptionMessage
    case e: FailedFastException =>
      e.getClass.getName
    case e: SourcedException =>
      stripNewlines(e)
    case e =>
      val msg = e.getMessage
      if (msg == null || msg.isEmpty)
        e.getClass.getName
      else
        e.getClass.getName + " " + msg
  }

  def toDetailedExceptionMessage(tryThrowable: Try[_]): String = tryThrowable match {
    case Throw(e) => toExceptionDetails(e) + " " + toExceptionMessage(e)
    case _ => ""
  }

  /**
   * Determines if the given [[Throwable]] represents a "Cancellation". "Cancellations" are not
   * typically meant to be retried as they represent an interrupt that signals the caller is
   * no longer listening for the response to the work being done. This method can be useful in
   * cases where users wish to "mask" cancellations to asynchronous work because they desire the
   * work to finish regardless of whether the caller is still listening. e.g., work which is
   * side-effecting and should not be interrupted once started.
   * @param t the [[Throwable]] to inspect to determine if it represents a "cancellation".
   * @return `true` if the given [[Throwable]] represents a "cancellation", `false` otherwise.
   * @see [[com.twitter.util.Future.mask]]
   * @see [[https://twitter.github.io/finagle/guide/FAQ.html?highlight=cancelled#what-are-cancelledrequestexception-cancelledconnectionexception-and-clientdiscardedrequestexception]]
   */
  @tailrec
  def isCancellation(t: Throwable): Boolean = t match {
    case _: CancelledRequestException => true
    case _: CancelledConnectionException => true
    case _: ClientDiscardedRequestException => true
    case f: FailureFlags[_] if f.isFlagged(FailureFlags.Ignorable) => true
    case f: FailureFlags[_] if f.isFlagged(FailureFlags.Interrupted) => true
    case f: Failure if f.cause.isDefined => isCancellation(f.cause.get)
    case _ => false
  }

  /**
   * Determines if the given [[Throwable]] represents an exception which should be considered as
   * signaling that the work which caused the exception should not be retried.
   * @param t the [[Throwable]] to inspect to determine if it represents a signal to not retry
   * @return `true` if the given [[Throwable]] represents an non-retryable exception, `false` otherwise.
   */
  @tailrec
  def isNonRetryable(t: Throwable): Boolean = t match {
    case _: NonRetryableException => true
    case f: FailureFlags[_] if f.isFlagged(FailureFlags.Ignorable) => true
    case f: Failure if f.cause.isDefined => isNonRetryable(f.cause.get)
    case _ => false
  }
}
