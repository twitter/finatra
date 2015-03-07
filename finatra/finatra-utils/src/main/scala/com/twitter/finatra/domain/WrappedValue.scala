package com.twitter.finatra.domain

/**
 * Marker interface for Case Classes wrapping a single value.
 * When used with finatra-jackson, the wrapped value will be directly serialized/deserialized without the wrapping object.
 *
 * WrappedValue is a Universal Trait (http://docs.scala-lang.org/overviews/core/value-classes.html)
 * so that it can be used by value classes.
 */
trait WrappedValue[T] extends Any {
  self: Product =>

  def onlyValue: T = self.productElement(0).asInstanceOf[T]

  def asString =
    onlyValue.toString

  // NOTE: We'd rather not override toString, but this is currently required for Jackson to handle WrappedValue's used as Map keys
  override def toString = asString
}
