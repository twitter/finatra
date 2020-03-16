package com.twitter.finatra.validation

import java.lang.annotation.Annotation

/**
 * A ConstraintValidator to validate the given value of type [[T]] against a particular annotation
 * of type [[A]].
 *
 * @param messageResolver to resolve error messages when validation fails. Users can use or
 *                        extend the [[messageResolver]] that is provided by default.
 * @tparam T the target type of the value to validate against the annotation object.
 */
abstract class ConstraintValidator[A <: Annotation, T](
  messageResolver: MessageResolver) {

  /**
   * Validate if a value meets the criteria defined in an annotation. E.g. Users can define a `Max`
   * annotation as `Max(value = 9)`, and verify in the method if the given value is less than or
   * equal to 9.
   *
   * @param value of type [[T]].
   * @param annotation an [[Annotation]] of type [[A]].
   * @return a [[ValidationResult]], either [[ValidationResult.Valid]] or
   *         [[ValidationResult.Invalid]] along with validation errors.
   */
  def isValid(annotation: A, value: T): ValidationResult
}
