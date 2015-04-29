package com.twitter.finatra.json.internal.caseclass.exceptions

case class JsonMethodValidationException(
  msg: String)
  extends Exception(msg)
