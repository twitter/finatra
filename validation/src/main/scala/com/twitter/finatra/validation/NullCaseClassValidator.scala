package com.twitter.finatra.validation

import java.lang.annotation.Annotation

/**
 * No-op validator, which will treat any field values as acceptable during
 * application of validation annotations.
 */
private[validation] object NullCaseClassValidator extends CaseClassValidator {

  override def validateField[V](
    fieldValue: V,
    fieldValidationAnnotations: Array[Annotation]
  ): Seq[ValidationResult] = Seq.empty

  override def validateMethods(obj: Any): Seq[ValidationResult] = Seq.empty
}
