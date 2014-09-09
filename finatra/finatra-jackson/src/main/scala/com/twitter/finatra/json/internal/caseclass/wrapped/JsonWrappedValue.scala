package com.twitter.finatra.json.internal.caseclass.wrapped

/**
 * Marker interface for Case Classes wrapping a single value that wish for the value to be directly
 * serialized/deserialized without the wrapping object
 */
trait JsonWrappedValue[T] {
  self: Product =>

  assert(self.productArity == 1, "JsonWrappedValue can only be used on Case Classes w/ a single value")
  private[finatra] val onlyValue: T = self.productElement(0).asInstanceOf[T]

  def asString =
    onlyValue.toString

  // I'd like to move this usage to asString (and retain a proper toString),
  // but overriding toString is required for seemless "Map key" JSON serialization
  override def toString = asString
}