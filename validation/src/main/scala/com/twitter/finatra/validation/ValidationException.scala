package com.twitter.finatra.validation

/**
 * Used to signal validation errors during case class validations.
 *
 * @param results Per-field details (of type [[ValidationResult]]) are carried to
 *                       provide the ability to iterate over all invalid results from validating a
 *                       case class field or a case class method.
 */
class ValidationException private[validation] (results: Seq[ValidationResult])
    extends Exception {

  /** All errors encountered during case class validations of each annotation */
  val errors: Seq[ValidationResult.Invalid] =
    results.asInstanceOf[Seq[ValidationResult.Invalid]].sortBy(_.message)

  override def getMessage: String = {
    "\nValidation Errors:\t\t" + errors.map(_.message).mkString(", ") + "\n\n"
  }
}
