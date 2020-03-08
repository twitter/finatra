package com.twitter.finatra.jackson.caseclass.exceptions

import com.google.inject.Key
import com.twitter.finatra.json.annotations.InjectableValue
import scala.util.control.NoStackTrace

object InjectableValuesException {
  def apply(parentClass: Class[_], fieldName: String) = new InjectableValuesException(
    s"Injection of @${classOf[InjectableValue].getSimpleName}-annotated case class fields is not " +
      s"supported for $fieldName in class $parentClass due to an invalid configuration. Please " +
      "check that your object mapper is properly configured to inject " +
      s"@${classOf[InjectableValue].getSimpleName}-annotated case class fields as " +
      "Jackson InjectableValues."
  )

  def apply(parentClass: Class[_], fieldName: String, key: Key[_], cause: Throwable) =
    new InjectableValuesException(
      s"Unable to inject field '$fieldName' with $key into class $parentClass",
      cause)
}

/**
 * Represents an exception during deserialization due to the inability to properly inject a value via
 * Jackson [[com.fasterxml.jackson.databind.InjectableValues]]. A common cause is an incorrectly
 * configured mapper that is not instantiated with an appropriate instance of a
 * [[com.fasterxml.jackson.databind.InjectableValues]] that can locate the requested value to inject
 * during deserialization.
 *
 * @note this exception is not handled during case class deserialization and is thus expected to be
 *       handled by callers.
 */
class InjectableValuesException private (message: String, cause: Throwable)
    extends Exception(message, cause)
    with NoStackTrace {

  def this(message: String) {
    this(message, null)
  }
}
