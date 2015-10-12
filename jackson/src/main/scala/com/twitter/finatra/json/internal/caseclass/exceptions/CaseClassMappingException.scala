package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

case class CaseClassMappingException(
  validationExceptions: Set[CaseClassValidationException] = Set())
  extends JsonMappingException("") {

  val errors = validationExceptions.toSeq.sortBy(_.getMessage)

  override def getMessage: String = {
    "\nErrors:\t\t" + errors.mkString(", ") + "\n\n"
  }
}
