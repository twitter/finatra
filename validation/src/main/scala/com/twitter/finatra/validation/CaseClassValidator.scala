package com.twitter.finatra.validation

import java.lang.annotation.Annotation

/**
 * Trait for defining a validator that will be triggered during Case Class validation.
 */
private[finatra] trait CaseClassValidator {

  /**
   * Validate a field's value according to the field's validation annotations
   *
   * @param fieldValue The value to be validated
   * @param fieldValidationAnnotations A list of annotations to validate on the field
   * @return A list of `ValidationResult`, each `ValidationResult` contains an annotation that has
   *         been validated and a boolean indicating if the annotation is valid
   */
  private[finatra] def validateField[V](
    fieldValue: V,
    fieldValidationAnnotations: Seq[Annotation]
  ): Seq[ValidationResult]

  /**
   * Validate an object using @MethodValidation annotated methods
   *
   * @param obj A case class object
   *
   * @return A list of `ValidationResult`, each `ValidationResult` contains an annotation that has
   *         been validated and a boolean indicating if the annotation is valid
   */
  def validateMethods(obj: Any): Seq[ValidationResult]

}
