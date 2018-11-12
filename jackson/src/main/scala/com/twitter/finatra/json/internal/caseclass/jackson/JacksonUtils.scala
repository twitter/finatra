package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.json.internal.caseclass.exceptions.FinatraJsonMappingException
import com.twitter.inject.Logging

object JacksonUtils extends Logging {

  /* Public */

  /**
   * Converts a JsonProcessingException to a informative error message that can be returned
   * to callers. The goal is to not leak internals of the underlying types.
   * @param e the [[JsonProcessingException]] to convert into a [[String]] message
   * @return a useful [[String]] error message
   */
  def errorMessage(e: JsonProcessingException): String = e match {
    case exc: FinatraJsonMappingException =>
      exc.getMessage // One of our deserializers/validations failed and we want to pass this message on to the user
    case exc: JsonMappingException =>
      exc.getMessage
    case _ if e.getCause == null =>
      e.getOriginalMessage // jackson threw the original error
    case _ =>
      e.getCause.getMessage // custom deserialization code threw the exception (e.g., enum deserialization)
  }
}
