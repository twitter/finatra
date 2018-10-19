package com.twitter.finatra.http.response

import com.twitter.finagle.http.{service => finagle}
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}

object HttpResponseClassifier {

  /**
   * An [[HttpResponseClassifier]] that categorizes responses with status codes in the 500s as
   * [[ResponseClass.NonRetryableFailure NonRetryableFailures]].
   *
   * @note that retryable NACKs are categorized as a [[ResponseClass.RetryableFailure]].
   * @see [[com.twitter.finagle.http.service.HttpResponseClassifier.ServerErrorsAsFailures]]
   */
  val ServerErrorsAsFailures =
    HttpResponseClassifier(
      finagle.HttpResponseClassifier.ServerErrorsAsFailures)
}

/**
 * A wrapper around a [[com.twitter.finagle.service.ResponseClassifier]] to allow
 * for binding a specific type of [[ResponseClassifier]] to the object graph.
 *
 * @param underlying the [[com.twitter.finagle.service.ResponseClassifier]] to wrap.
 */
case class HttpResponseClassifier(underlying: ResponseClassifier)
  extends PartialFunction[ReqRep, ResponseClass] {
  override def orElse[A1 <: ReqRep, B1 >: ResponseClass](
    that: PartialFunction[A1, B1]
  ): PartialFunction[A1, B1] = underlying.orElse(that)

  override def andThen[C](k: ResponseClass => C): PartialFunction[ReqRep, C] =
    underlying.andThen(k)

  override def lift: ReqRep => Option[ResponseClass] =
    underlying.lift

  override def applyOrElse[A1 <: ReqRep, B1 >: ResponseClass](
    v1: A1,
    default: A1 => B1
  ): B1 = underlying.applyOrElse(v1, default)

  override def runWith[U](action: ResponseClass => U): ReqRep => Boolean =
    underlying.runWith(action)

  def isDefinedAt(v1: ReqRep): Boolean = underlying.isDefinedAt(v1)

  override def apply(v1: ReqRep): ResponseClass = underlying.apply(v1)

  override def compose[A](g: A => ReqRep): A => ResponseClass = underlying.compose(g)

  override def toString(): String = underlying.toString()
}
