package com.twitter.finatra.jackson.caseclass

/**
 * Represents class types which should be "injected" during deserialization via the
 * Jackson [[com.fasterxml.jackson.databind.InjectableValues]] functionality. This does
 * not provide an API for injection of the values. Implementations of this
 * trait merely signals that fields of the given types should be considered to have values
 * that will be provided during deserialization rather than read from the incoming JSON.
 * How the value for the field is provided is defined by an implementation of the
 * [[com.fasterxml.jackson.databind.InjectableValues]].
 *
 * @see [[com.fasterxml.jackson.databind.InjectableValues]]
 */
private[finatra] trait InjectableTypes {
  def list: Seq[Class[_]]
}

private[jackson] object NullInjectableTypes extends InjectableTypes {
  def list: Seq[Class[_]] = Seq.empty[Class[_]]
}
