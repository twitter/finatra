package com.twitter.inject.exceptions

import scala.runtime.ScalaRunTime
import scala.util.control.NoStackTrace

class DetailedNonRetryableSourcedException(
  val message: String = "")
  extends Exception(message)
  with NoStackTrace
  with NonRetryableException {

  /**
   * The named component responsible for causing this exception.
   */
  val source: String = ""

  /**
   * Details about this exception suitable for stats. Each element will be converted into a string.
   * Note: Each element must have a bounded set of values (e.g. You can stat the type of a tweet
   * e.g., "protected" or "unprotected", but you shouldn't include an actual tweet id,
   * e.g. "121314151617181912131").
   *
   * This is different from message which will be logged when this exception occurs. Typically message contains
   * any unbounded details of the exception that you are not able to stat such as an actual tweet id (see above).
   */
  val details: Seq[Any] = Seq(this.getClass.getSimpleName)

  lazy val toDetailsString = {
    if (source.isEmpty)
      details.mkString("/")
    else
      source + "/" + details.mkString("/")
  }

  override def toString = this match {
    case product: Product =>
      ScalaRunTime._toString(product)
    case _ =>
      super.toString
  }
}