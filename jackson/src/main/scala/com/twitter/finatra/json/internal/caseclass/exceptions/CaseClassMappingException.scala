package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

case class CaseClassMappingException(
  validationExceptions: Set[CaseClassValidationException] = Set()
) extends JsonMappingException(null, "") {

  /**
   * The collection of [[CaseClassValidationException]] which make up this [[CaseClassMappingException]].
   * This collection is intended to be purposely exhaustive in that is specifies all errors
   * encountered in mapping JSON to a case class.
   */
  val errors: Seq[CaseClassValidationException] = validationExceptions.toSeq.sortBy(_.getMessage)

  override def getMessage: String = {
    "\nErrors:\t\t" + errors.mkString(", ") + "\n\n"
  }
}
