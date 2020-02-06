package com.twitter.finatra.jackson.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.validation.ValidationResult

object CaseClassFieldMappingException {
  object PropertyPath {
    val Empty: PropertyPath = PropertyPath(Seq.empty)
    private val FieldSeparator = "."
    private[finatra] def leaf(name: String): PropertyPath = Empty.withParent(name)
  }

  /** Represents a path to the case class field/property germane to the exception */
  case class PropertyPath(names: Seq[String]) {
    private[jackson] def withParent(name: String): PropertyPath = copy(name +: names)

    def isEmpty: Boolean = names.isEmpty
    def prettyString: String = names.mkString(PropertyPath.FieldSeparator)
  }
}

/**
 * A subclass of [[JsonMappingException]] which bundles together a failed field location as a
 * `CaseClassFieldMappingException.PropertyPath` with the the failure reason represented by a
 * [[ValidationResult.Invalid]] to carry the failure reason.
 *
 * @note this exception is a case class in order to have a useful equals() and hashcode()
 *       methods since this exception is generally carried in a collection inside of a
 *       [[CaseClassMappingException]].
 *
 * @param path - a `CaseClassFieldMappingException.PropertyPath` instance to the case class field
 *             that caused the failure.
 * @param reason - an instance of a [[ValidationResult.Invalid]] which carries details of the
 *               failure reason.
 *
 * @see [[com.twitter.finatra.jackson.caseclass.exceptions.CaseClassFieldMappingException.PropertyPath]]
 * @see [[com.twitter.finatra.validation.ValidationResult.Invalid]]
 * @see [[com.fasterxml.jackson.databind.JsonMappingException]]
 * @see [[CaseClassMappingException]]
 */
case class CaseClassFieldMappingException(
  path: CaseClassFieldMappingException.PropertyPath,
  reason: ValidationResult.Invalid)
    extends JsonMappingException(null, reason.message) {

  /* Public */

  /**
   * Render a human readable message. If the error message pertains to
   * a specific field it is prefixed with the field's name.
   */
  override def getMessage: String = {
    if (path == null || path.isEmpty) reason.message
    else s"${path.prettyString}: ${reason.message}"
  }

  /* Private */

  /** fill in any missing PropertyPath information */
  private[jackson] def withPropertyPath(
    path: CaseClassFieldMappingException.PropertyPath
  ): CaseClassFieldMappingException = this.copy(path)

  private[jackson] def scoped(fieldName: String): CaseClassFieldMappingException = {
    copy(path = path.withParent(fieldName))
  }
}
