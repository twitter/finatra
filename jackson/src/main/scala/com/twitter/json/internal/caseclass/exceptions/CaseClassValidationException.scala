package com.twitter.finatra.json.internal.caseclass.exceptions

import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassValidationException.FieldSeparator
import com.twitter.finatra.json.internal.caseclass.jackson.CaseClassField
import com.twitter.finatra.validation.ValidationResult

object CaseClassValidationException {
  private val FieldSeparator = "."
}

/**
 * An exception that bundles together a failed validation with the
 * associated field that failed the validation.
 * @param fieldPath - path to the case class field that caused the validation failure
 * @param reason - the validation failure
 */
case class CaseClassValidationException(
  fieldPath: Seq[String],
  reason: ValidationResult.Invalid)
  extends Exception {

  /**
   * Render a human readable message. If the error message pertains to
   * a specific field it is prefixed with the field's name.
   */
  override def getMessage: String = {
    if (fieldPath.isEmpty) {
      reason.message
    } else {
      val field = fieldPath.mkString(FieldSeparator)
      s"${field}: ${reason.message}"
    }
  }

  private[finatra] def scoped(field: CaseClassField): CaseClassValidationException = {
    copy(fieldPath = field.name +: fieldPath)
  }
}
