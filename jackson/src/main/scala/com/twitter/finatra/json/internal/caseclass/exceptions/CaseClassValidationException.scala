package com.twitter.finatra.json.internal.caseclass.exceptions

import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassValidationException.FieldSeparator
import com.twitter.finatra.json.internal.caseclass.jackson.CaseClassField
import com.twitter.finatra.validation.ValidationResult

object CaseClassValidationException {
  private val FieldSeparator = "."
}

/**
 * An exception that bundles together a failed validation with the
 * associated field that failed the validation.  An empty `field`
 * string means the top-level object failed the (method) validation.
 * Otherwise, `field` is a dot-separated path to a particular case
 * class field.
 */
case class CaseClassValidationException(
  field: String,
  reason: ValidationResult.Invalid)
  extends Exception {

  /**
   * Render a human readable message. If the error message pertains to
   * a specific field it is prefixed with the field's name.
   */
  override def getMessage: String = {
    if (field.isEmpty) reason.message
    else s"${field}: ${reason.message}"
  }

  private[finatra] def nest(outerField: CaseClassField): CaseClassValidationException = {
    CaseClassValidationException(outerField.name + FieldSeparator + field,  reason)
  }
}
