package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

/**
  * Exception for handling Finatra-specific errors that may otherwise be valid
  * in the eyes of Jackson.
  * @param msg gets presented to the end user, so don't leak implementation details.
  */
class FinatraJsonMappingException(
  msg: String)
  extends JsonMappingException(msg) {
}
