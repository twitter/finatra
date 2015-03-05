package com.twitter.finatra.domain

/**
 * Marker interface for Case Classes wrapping a single value.
 * When used with finatra-jackson, the wrapped value will be directly serialized/deserialized without the wrapping object.
 */
trait WrappedValue[T] {
  self: Product =>

  assert(self.productArity == 1, "WrappedValue can only be used with single field case classes")
  val onlyValue: T = self.productElement(0).asInstanceOf[T]

  def asString =
    onlyValue.toString

  // NOTE: We'd rather not override toString, but this is currently required for Jackson to handle WrappedValue's used as Map keys
  override def toString = asString
}
