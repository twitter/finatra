package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.json.internal.caseclass.exceptions.FinatraJsonMappingException

object JacksonUtils {

  /* Public */
  def errorMessage(e: JsonProcessingException): String = e match {
    case fjme: FinatraJsonMappingException =>
      fjme.getMessage // One of our deserializers/validations failed and we want to pass this message on to the user
    case jme: JsonMappingException =>
      "Unable to parse" // Prevent JsonMappingException from leaking implementation details
    case _ if e.getCause == null =>
      e.getOriginalMessage // jackson threw the original error
    case _ =>
      e.getCause.getMessage // custom deserialization code threw the exception (e.g., enum deserialization)
  }
}
