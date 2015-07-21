package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

case class JsonObjectParseException(
  fieldErrors: Seq[JsonFieldParseException] = Seq(),
  methodValidationErrors: Seq[JsonMethodValidationException] = Seq())
  extends JsonMappingException("") {

  override def getMessage: String = {
    "\nFieldErrors:\t\t" + fieldErrors.mkString(", ") +
      "\nMethodValidationErrors:\t" + methodValidationErrors.mkString(", ") + "\n\n"
  }
}