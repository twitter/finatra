package com.twitter.finatra.validation

import scala.util.control.NoStackTrace

/**
 * Used to signal validation errors during case class validations.
 *
 * @param includeFieldNames If field names should be included in the carried per-field details
 *                          message field. In the stand alone (post-construction) Validation
 *                          Framework API case, this will be true to provide fidelity in error
 *                          reporting. In the Jackson CaseClassDeserializer case this will be
 *                          false as this information is carried in the deserializer and does not
 *                          need to be repeated in the message field.
 * @param results Per-field details (of type [[ValidationResult]]) are carried to
 *                provide the ability to iterate over all invalid results from validating a
 *                case class field or a case class method.
 */
class ValidationException private[validation] (
  includeFieldNames: Boolean,
  results: Set[ValidationResult])
    extends Exception
    with NoStackTrace {

  private[validation] def this(results: Set[ValidationResult]) {
    this(false, results)
  }

  /** All errors encountered during case class validations of each annotation */
  val errors: Seq[ValidationResult.Invalid] = {
    if (includeFieldNames) {
      results
        .asInstanceOf[Set[ValidationResult.Invalid]].toSeq
        .map(e => e.copy(message = s"${e.path.toString}: ${e.message}"))
        .sortBy(_.message)
    } else {
      results.asInstanceOf[Set[ValidationResult.Invalid]].toSeq.sortBy(_.message)
    }
  }

  override def getMessage: String = {
    "\nValidation Errors:\t\t" + errors.map(_.message).mkString(", ") + "\n\n"
  }
}
