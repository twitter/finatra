package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.JsonProcessingException

object JacksonUtils {

  def errorMessage(e: JsonProcessingException): String = {
    if (e.getCause == null)
      e.getOriginalMessage // jackson threw the original error
    else
      e.getCause.getMessage // custom deserialization code threw the exception (e.g., enum deserialization)
  }
}
