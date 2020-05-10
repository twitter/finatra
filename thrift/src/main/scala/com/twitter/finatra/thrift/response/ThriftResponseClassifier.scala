package com.twitter.finatra.thrift.response

import com.twitter.finagle.thrift.{service => finagle}
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}

object ThriftResponseClassifier {

  /**
   * A [[ThriftResponseClassifier]] that categorizes responses where the '''deserialized''' response
   * is a Thrift Exception as a `ResponseClass.NonRetryableFailure`.
   *
   * @see [[com.twitter.finagle.thrift.service.ThriftResponseClassifier.ThriftExceptionsAsFailures]]
   */
  val ThriftExceptionsAsFailures =
    ThriftResponseClassifier(finagle.ThriftResponseClassifier.ThriftExceptionsAsFailures)
}

/**
 * A wrapper around a [[com.twitter.finagle.service.ResponseClassifier]] to allow
 * for binding a specific type of [[ResponseClassifier]] to the object graph.
 *
 * @param underlying the [[com.twitter.finagle.service.ResponseClassifier]] to wrap.
 */
case class ThriftResponseClassifier(underlying: ResponseClassifier)
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
