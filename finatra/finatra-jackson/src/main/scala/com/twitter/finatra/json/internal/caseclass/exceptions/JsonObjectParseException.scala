package com.twitter.finatra.json.internal.caseclass.exceptions

case class JsonObjectParseException(
  fieldErrors: Seq[JsonFieldParseException] = Seq(),
  methodValidationErrors: Seq[JsonMethodValidationException] = Seq())
  extends Exception {

  override def getMessage: String = {
    "\nFieldErrors:\t\t" + fieldErrors.mkString(", ") +
      "\nMethodValidationErrors:\t" + methodValidationErrors.mkString(", ") + "\n\n"
  }
}