package com.twitter.finatra.json.internal.caseclass.validation

import com.twitter.finatra.validation.ValidationResult
import java.lang.annotation.Annotation

/**
 * Trait for defining a validator that will be triggered during Case Class validation.
 */
private[finatra] trait CaseClassValidator {

  def validateField[V](
    fieldValue: V,
    fieldValidationAnnotations: Seq[Annotation]
  ): Seq[ValidationResult]

  def validateObject(obj: Any): Seq[ValidationResult]

}
