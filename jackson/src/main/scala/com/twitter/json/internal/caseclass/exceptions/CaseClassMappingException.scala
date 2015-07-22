package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

case class CaseClassMappingException(
  errors: Seq[CaseClassValidationException] = Seq())
  extends JsonMappingException("") {

  override def getMessage: String = {
    "\nErrors:\t\t" + errors.mkString(", ") +"\n\n"
  }
}
