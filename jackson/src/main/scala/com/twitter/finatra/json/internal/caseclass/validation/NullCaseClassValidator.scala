package com.twitter.finatra.json.internal.caseclass.validation

import com.twitter.finatra.validation.ValidationResult
import java.lang.annotation.Annotation

/**
 * No-op validator, which will treat any field values as acceptable during
 * Finatra Validation of annotations.
 */
private[json] object NullCaseClassValidator extends CaseClassValidator {

  override def validateField[V](
    fieldValue: V,
    fieldValidationAnnotations: Seq[Annotation]
  ): Seq[ValidationResult] = Seq.empty

  override def validateObject(obj: Any): Seq[ValidationResult] = Seq.empty
}
