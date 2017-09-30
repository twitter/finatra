package com.twitter.finatra.json.internal.caseclass.exceptions

import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassValidationException.PropertyPath
import com.twitter.finatra.json.internal.caseclass.jackson.CaseClassField
import com.twitter.finatra.validation.ValidationResult

object CaseClassValidationException {

  object PropertyPath {
    val empty = PropertyPath(Seq.empty)

    private val FieldSeparator = "."

    private[finatra] def leaf(name: String): PropertyPath = empty.withParent(name)
  }

  case class PropertyPath private (names: Seq[String]) {
    private[finatra] def withParent(name: String): PropertyPath = copy(name +: names)

    def isEmpty: Boolean = names.isEmpty
    def prettyString: String = names.mkString(PropertyPath.FieldSeparator)
  }
}

/**
 * An exception that bundles together a failed validation with the
 * associated field that failed the validation.
 * @param path - path to the case class field that caused the validation failure
 * @param reason - the validation failure
 */
case class CaseClassValidationException(path: PropertyPath, reason: ValidationResult.Invalid)
    extends Exception {

  /**
   * Render a human readable message. If the error message pertains to
   * a specific field it is prefixed with the field's name.
   */
  override def getMessage: String = {
    if (path.isEmpty) reason.message
    else s"${path.prettyString}: ${reason.message}"
  }

  private[finatra] def scoped(field: CaseClassField): CaseClassValidationException = {
    copy(path = path.withParent(field.name))
  }
}
